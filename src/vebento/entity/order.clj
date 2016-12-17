(ns vebento.entity.order
  (:require [clojure.future
             :refer :all]
            [clojure.spec
             :as s]
            [com.stuartsierra.component
             :as co]
            [juncture.componad
             :as componad
             :refer [within]]
            [juncture.util
             :as j-util
             :refer [ns-alias]]
            [juncture.core
             :as ju
             :refer []]
            [juncture.event
             :as event
             :refer []]
            [juncture.entity
             :as entity
             :refer [create transform def-entity]]
            [vebento.specs
             :as specs]))


(ns-alias 'customer 'vebento.entity.customer)
(ns-alias 'retailer 'vebento.entity.retailer)
(ns-alias 'product 'vebento.entity.product)


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
