(ns vebento.order
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
             :refer [def-command def-message def-failure
                     subscribe* unsubscribe*]]
            [juncture.entity
             :as entity
             :refer [register unregister def-entity create transform]]
            [componad
             :refer [within]]
            [vebento.core
             :refer [aggregate publish execute fail-with fail-if-exists
                     fail-unless-exists transform-in get-entity]]
            [vebento.specs
             :as specs]))


(ns-alias 'customer 'vebento.customer)
(ns-alias 'product 'vebento.product)
(ns-alias 'merchant 'vebento.merchant)


(s/def ::id ::specs/id)
(s/def ::items (s/and ::specs/map (comp not empty?)))
(s/def ::address ::specs/address)
(s/def ::schedule (s/and ::specs/set (comp not empty?)))
(s/def ::payment-method ::specs/payment-method)
(s/def ::paid ::specs/paid)
(s/def ::shipped ::specs/shipped)


(def-message ::placed
  :req [::id
        ::customer/id
        ::merchant/id
        ::items
        ::address
        ::schedule
        ::payment-method])


(def-entity ::entity
  :req [::customer/id
        ::merchant/id
        ::items
        ::address
        ::schedule
        ::payment-method
        ::paid
        ::shipped])


(defmethod transform
  [nil ::placed]
  [_ {order-id ::id
      merchant-id ::merchant/id
      customer-id ::customer/id
      items ::items
      address ::address
      schedule ::schedule
      payment-method ::payment-method}]
  (create ::entity
          ::entity/id order-id
          ::merchant/id merchant-id
          ::customer/id customer-id
          ::items items
          ::address address
          ::schedule schedule
          ::payment-method payment-method
          ::paid false
          ::shipped false))


(defrecord Component

  [aggregates dispatcher journal entity-store subscriptions]

  co/Lifecycle

  (start [this]

    (register aggregates [::processing])

    (assoc
      this :subscriptions
      (subscribe*
        dispatcher
        [::event/type ::placed
         (transform-in entity-store ::id)])))

  (stop [this]
    (apply unsubscribe* dispatcher subscriptions)
    (assoc this :subscriptions nil)))
