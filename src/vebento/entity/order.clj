(ns vebento.entity.order
  (:require [clojure.future
             :refer :all]
            [clojure.spec
             :as s]
            [com.stuartsierra.component
             :as co]
            [juncture.componad
             :refer [within]]
            [juncture.core
             :as ju
             :refer [fail-if
                     fail-if-exists
                     fail-unless-exists]]
            [juncture.util
             :refer [ns-alias not-in?]]
            [juncture.event
             :refer [publish
                     def-subscription
                     def-command
                     def-message
                     def-failure]]
            [juncture.entity
             :as entity
             :refer [create
                     transform
                     get-entity
                     def-entity
                     def-aggregate
                     aggregate]]
            [vebento.specs
             :as specs]))


(ns-alias 'specs 'vebento.specs)
(ns-alias 'customer 'vebento.entity.customer)
(ns-alias 'retailer 'vebento.entity.retailer)
(ns-alias 'product 'vebento.entity.product)


(s/def ::address ::spec/address)
(s/def ::items ::spec/items)
(s/def ::schedule ::spec/schedule)
(s/def ::payment-method ::spec/payment-method)
(s/def ::paid ::spec/paid)
(s/def ::shipped ::spec/shipped)



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
  (entity/create ::entity
                 ::entity/id order-id
                 ::retailer/id retailer-id
                 ::customer/id customer-id
                 ::address address
                 ::items items
                 ::schedule schedule
                 ::payment-method payment-method
                 ::paid false
                 ::shipped false))
