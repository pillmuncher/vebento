(ns vebento.customer.clears-cart
  (:require [util
             :refer [ns-alias]]
            [juncture.event
             :as event
             :refer [defcommand defmessage message]]
            [juncture.entity
             :as entity
             :refer [transform transform-in]]
            [componad
             :refer [mdo-within]]
            [vebento.core
             :refer [boundary fail-unless-exists call post fail]]))


(ns-alias 'customer 'vebento.customer)


(defcommand ::customer/clear-cart
  :req [::customer/id])


(defmessage ::customer/cart-cleared
  :req [::customer/id])


(defmethod transform
  [::customer/entity ::customer/cart-cleared]
  [customer _]
  (assoc customer ::customer/cart {}))


(defn subscriptions
  [component]

  {::customer/cart-cleared
   [(transform-in (:repository component) ::customer/id)]

   ::customer/clear-cart
   [(fn [{customer-id ::customer/id}]
      (mdo-within (boundary component #{::customer/shop})
        (fail-unless-exists ::customer/id customer-id)
        (post ::customer/cart-cleared
              ::customer/id customer-id)))]})
