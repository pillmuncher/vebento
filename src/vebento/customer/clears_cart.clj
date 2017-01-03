(ns vebento.customer.clears-cart
  (:require [clojure.future
             :refer :all]
            [util
             :refer [ns-alias]]
            [juncture.event
             :as event
             :refer [def-command def-message]]
            [juncture.entity
             :as entity
             :refer [transform]]
            [componad
             :refer [within]]
            [vebento.core
             :refer [aggregate publish transform-in get-entity
                     fail-unless-exists]]))


(ns-alias 'customer 'vebento.customer)


(def-command ::customer/clear-cart
  :req [::customer/id])


(def-message ::customer/cart-cleared
  :req [::customer/id])


(defmethod transform
  [::customer/entity ::customer/cart-cleared]
  [customer _]
  (assoc customer ::customer/cart {}))


(defn subscriptions
  [component]

  [[::event/type ::customer/cart-cleared
    (transform-in (:entity-store component) ::customer/id)]

   [::event/type ::customer/clear-cart
    (fn [{customer-id ::customer/id}]
      (within (aggregate component [::customer/shopping] customer-id)
        (fail-unless-exists ::customer/id customer-id)
        (publish ::customer/cart-cleared
                 ::customer/id customer-id)))]])
