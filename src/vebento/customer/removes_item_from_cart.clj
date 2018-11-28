(ns vebento.customer.removes-item-from-cart
  (:require [monads.util
             :refer [mwhen]]
            [util
             :refer [ns-alias not-in?]]
            [juncture.event
             :as event
             :refer [defcommand defmessage deffailure message failure]]
            [juncture.entity
             :as entity
             :refer [promote promote-in]]
            [componad
             :refer [mdo-within]]
            [vebento.core
             :refer [boundary get-entity call post fail]]))


(ns-alias 'product 'vebento.product)
(ns-alias 'customer 'vebento.customer)


(defcommand ::customer/remove-item-from-cart
  :req [::customer/id
        ::product/id])


(defmessage ::customer/item-removed-from-cart
  :req [::customer/id
        ::product/id])


(deffailure ::customer/product-not-in-cart
  :req [::customer/id
        ::product/id])


(defmethod promote
  [::customer/entity ::customer/item-removed-from-cart]
  [customer {product-id ::product/id}]
  (update customer ::customer/cart dissoc product-id))


(defn subscriptions
  [component]

  {::customer/item-removed-from-cart
   [(promote-in (:repository component) ::customer/id)]

   ::customer/remove-item-from-cart
   [(fn [{customer-id ::customer/id
          product-id ::product/id}]
      (mdo-within (boundary component #{::customer/shop})
        customer <- (get-entity ::customer/id customer-id)
        (mwhen (-> product-id
                   (not-in? (@customer ::customer/cart)))
               (fail ::customer/product-not-in-cart
                     ::customer/id customer-id
                     ::product/id product-id))
        (post ::customer/item-removed-from-cart
              ::customer/id customer-id
              ::product/id product-id)))]})
