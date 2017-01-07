(ns vebento.customer
  (:require [clojure.future
             :refer :all]
            [clojure.spec
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
            [vebento.customer.registers
             :as registers]
            [vebento.customer.changes-address
             :as changes-address]
            [vebento.customer.selects-merchant
             :as selects-merchant]
            [vebento.customer.adds-schedule
             :as adds-schedule]
            [vebento.customer.selects-payment-method
             :as selects-payment-method]
            [vebento.customer.adds-item-to-cart
             :as adds-item-to-cart]
            [vebento.customer.removes-item-from-cart
             :as removes-item-from-cart]
            [vebento.customer.places-order
             :as places-order]
            [vebento.customer.clears-cart
             :as clears-cart]))


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
  [boundaries dispatcher journal entity-store subscriptions]
  co/Lifecycle
  (start [this]
    (register boundaries [::account ::shopping])
    (assoc this :subscriptions
           (subscribe-maps
             dispatcher
             (registers/subscriptions this)
             (changes-address/subscriptions this)
             (selects-merchant/subscriptions this)
             (adds-schedule/subscriptions this)
             (selects-payment-method/subscriptions this)
             (adds-item-to-cart/subscriptions this)
             (removes-item-from-cart/subscriptions this)
             (places-order/subscriptions this)
             (clears-cart/subscriptions this))))
  (stop [this]
    (apply unsubscribe* dispatcher subscriptions)
    (unregister boundaries [::account ::shopping])
    (assoc this :subscriptions nil)))
