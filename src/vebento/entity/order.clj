(ns vebento.entity.order
  (:require [clojure.future
             :refer :all]
            [clojure.set
             :refer [union intersection]]
            [clojure.spec
             :as s]
            [com.stuartsierra.component
             :as co]
            [monads.util
             :refer [mwhen]]
            [util
             :refer [ns-alias not-in?]]
            [juncture.event
             :as event
             :refer [def-command def-message def-failure
                     subscribe unsubscribe store]]
            [juncture.entity
             :as entity
             :refer [def-entity create transform]]
            [componad
             :refer [within]]
            [vebento.core
             :refer [def-aggregate aggregate publish execute fail-with
                     fail-if-exists fail-unless-exists f-mwhen get-entity]]
            [vebento.specs
             :as specs]))


(ns-alias 'customer 'vebento.entity.customer)
(ns-alias 'product 'vebento.entity.product)
(ns-alias 'retailer 'vebento.entity.retailer)


(s/def ::address ::specs/address)
(s/def ::items ::specs/items)
(s/def ::schedule ::specs/schedule)
(s/def ::payment-method ::specs/payment-method)
(s/def ::paid ::specs/paid)
(s/def ::shipped ::specs/shipped)



(def-entity ::entity
  :req [::retailer/id
        ::customer/id
        ::address
        ::items
        ::schedule
        ::payment-method
        ::paid
        ::shipped])


(defmethod transform
  [nil ::customer/order-placed]
  [_ {order-id ::id
      retailer-id ::retailer/id
      customer-id ::customer/id
      address ::address
      items ::items
      schedule ::schedule
      payment-method ::payment-method}]
  (create ::entity
          ::entity/id order-id
          ::retailer/id retailer-id
          ::customer/id customer-id
          ::address address
          ::items items
          ::schedule schedule
          ::payment-method payment-method
          ::paid false
          ::shipped false))
