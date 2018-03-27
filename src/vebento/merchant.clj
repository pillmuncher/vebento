(ns vebento.merchant
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
             :refer [register unregister def-entity transform transform-in]]
            [vebento.core
             :as core]
            [vebento.specs
             :as specs]
            [vebento.merchant.registers]
            [vebento.merchant.adds-area]
            [vebento.merchant.adds-product]
            [vebento.merchant.adds-schedule]
            [vebento.merchant.adds-payment-method]))


(ns-alias 'customer 'vebento.customer)
(ns-alias 'order 'vebento.order)


(s/def ::id ::specs/id)
(s/def ::address ::specs/address)
(s/def ::areas ::specs/set)
(s/def ::products ::specs/set)
(s/def ::schedule ::specs/schedule)
(s/def ::customers ::specs/set)
(s/def ::pending-orders ::specs/set)
(s/def ::payment-method ::specs/payment-method)
(s/def ::payment-methods ::specs/set)
(s/def ::schedule-time-of-day string?)
(s/def ::schedule-is-recurrent string?)


(def-entity ::entity
  :req [::address
        ::areas
        ::products
        ::schedule
        ::payment-methods
        ::customers
        ::pending-orders])


(defmethod transform
  [::entity ::customer/merchant-selected]
  [merchant {customer-id ::customer/id}]
  (update merchant ::customers conj customer-id))

(defmethod transform
  [::entity ::order/placed]
  [merchant {order-id ::order/id}]
  (update merchant ::customer/pending-orders conj order-id))


(defrecord Component
  [boundaries repository journal dispatcher subscriptions]
  co/Lifecycle
  (start [this]
    (register boundaries [::account])
    (assoc this :subscriptions
           (subscribe-maps
             dispatcher
             {::customer/merchant-selected
              [(transform-in repository ::id)]
              ::order/placed
              [(transform-in repository ::id)]}
             (vebento.merchant.registers/subscriptions this)
             (vebento.merchant.adds-area/subscriptions this)
             (vebento.merchant.adds-product/subscriptions this)
             (vebento.merchant.adds-schedule/subscriptions this)
             (vebento.merchant.adds-payment-method/subscriptions this))))
  (stop [this]
    (apply unsubscribe* dispatcher subscriptions)
    (unregister boundaries [::account])
    (assoc this :subscriptions nil)))
