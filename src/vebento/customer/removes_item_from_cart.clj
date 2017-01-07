(ns vebento.customer.removes-item-from-cart
  (:require [clojure.future
             :refer :all]
            [monads.util
             :refer [mwhen]]
            [util
             :refer [ns-alias not-in?]]
            [juncture.event
             :as event
             :refer [def-command def-message def-failure store-in]]
            [juncture.entity
             :as entity
             :refer [transform]]
            [componad
             :refer [within]]
            [vebento.core
             :refer [boundary publish fail-with transform-in get-entity]]))


(ns-alias 'product 'vebento.product)
(ns-alias 'customer 'vebento.customer)


(def-command ::customer/remove-item-from-cart
  :req [::customer/id
        ::product/id])


(def-message ::customer/item-removed-from-cart
  :req [::customer/id
        ::product/id])


(def-failure ::customer/product-not-in-cart
  :req [::customer/id
        ::product/id])


(defmethod transform
  [::customer/entity ::customer/item-removed-from-cart]
  [customer {product-id ::product/id}]
  (update customer ::customer/cart dissoc product-id))


(defn subscriptions
  [component]

  {::customer/remove-item-from-cart
   [(store-in (:journal component))
    (fn [{customer-id ::customer/id
          product-id ::product/id}]
      (within (boundary component #{::customer/shopping})
        customer <- (get-entity ::customer/id customer-id)
        (mwhen (->> product-id
                    (not-in? (@customer ::customer/cart)))
               (fail-with ::customer/product-not-in-cart
                          ::customer/id customer-id
                          ::product/id product-id))
        (publish ::customer/item-removed-from-cart
                 ::customer/id customer-id
                 ::product/id product-id)))]

   ::customer/item-removed-from-cart
   [(store-in (:journal component))
    (transform-in (:entity-store component) ::customer/id)]

   ::customer/product-not-in-cart
   [(store-in (:journal component))]})
