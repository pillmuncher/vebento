(ns vebento.customer
  (:require [clojure.spec.alpha
             :as s]
            [com.stuartsierra.component
             :as co]
            [util
             :refer [ns-alias]]
            [juncture.event
             :as event
             :refer [subscribe-maps unsubscribe*]]
            [juncture.entity
             :as entity
             :refer [register unregister def-entity]]
            [vebento.specs
             :as specs]
            [vebento.customer.registers]
            [vebento.customer.changes-address]
            [vebento.customer.selects-merchant]
            [vebento.customer.adds-schedule]
            [vebento.customer.selects-payment-method]
            [vebento.customer.adds-item-to-cart]
            [vebento.customer.removes-item-from-cart]
            [vebento.customer.places-order]
            [vebento.customer.clears-cart]))


(ns-alias 'merchant 'vebento.merchant)


(s/def ::id ::specs/id)
(s/def ::address ::specs/address)
(s/def ::cart ::specs/cart)
(s/def ::pending-orders ::specs/set)
(s/def ::schedule ::specs/schedule)
(s/def ::payment-method ::specs/payment-method)


(def-entity ::entity
  :req [::cart
        ::schedule
        ::pending-orders]
  :opt [::address
        ::merchant/id
        ::payment-method])


(defrecord Component
  [boundaries repository journal dispatcher subscriptions]
  co/Lifecycle
  (start [this]
    (register boundaries [::account ::shop])
    (assoc this :subscriptions
           (subscribe-maps
             dispatcher
             (vebento.customer.registers/subscriptions this)
             (vebento.customer.changes-address/subscriptions this)
             (vebento.customer.selects-merchant/subscriptions this)
             (vebento.customer.adds-schedule/subscriptions this)
             (vebento.customer.selects-payment-method/subscriptions this)
             (vebento.customer.adds-item-to-cart/subscriptions this)
             (vebento.customer.removes-item-from-cart/subscriptions this)
             (vebento.customer.places-order/subscriptions this)
             (vebento.customer.clears-cart/subscriptions this))))
  (stop [this]
    (apply unsubscribe* dispatcher subscriptions)
    (unregister boundaries [::account ::shop])
    (assoc this :subscriptions nil)))
