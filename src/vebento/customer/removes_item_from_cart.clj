(ns vebento.customer.removes-item-from-cart
  (:require [monads.util
             :refer [mwhen]]
            [util
             :refer [ns-alias not-in?]]
            [juncture.event
             :as event
             :refer [def-command def-notice def-error]]
            [juncture.entity
             :as entity
             :refer [transform transform-in]]
            [componad
             :refer [mdo-within]]
            [vebento.core
             :refer [boundary publish raise get-entity]]))


(ns-alias 'product 'vebento.product)
(ns-alias 'customer 'vebento.customer)


(def-command ::customer/remove-item-from-cart
  :req [::customer/id
        ::product/id])


(def-notice ::customer/item-removed-from-cart
  :req [::customer/id
        ::product/id])


(def-error ::customer/product-not-in-cart
  :req [::customer/id
        ::product/id])


(defmethod transform
  [::customer/entity ::customer/item-removed-from-cart]
  [customer {product-id ::product/id}]
  (update customer ::customer/cart dissoc product-id))


(defn subscriptions
  [component]

  {::customer/item-removed-from-cart
   [(transform-in (:repository component) ::customer/id)]

   ::customer/remove-item-from-cart
   [(fn [{customer-id ::customer/id
          product-id ::product/id}]
      (mdo-within (boundary component #{::customer/shop})
        customer <- (get-entity ::customer/id customer-id)
        (mwhen (-> product-id
                   (not-in? (@customer ::customer/cart)))
               (raise ::customer/product-not-in-cart
                      ::customer/id customer-id
                      ::product/id product-id))
        (publish ::customer/item-removed-from-cart
                 ::customer/id customer-id
                 ::product/id product-id)))]})
