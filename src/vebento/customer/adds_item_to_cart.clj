(ns vebento.customer.adds-item-to-cart
  (:require [clojure.future
             :refer :all]
            [monads.util
             :refer [mwhen]]
            [util
             :refer [ns-alias not-in?]]
            [juncture.event
             :as event
             :refer [def-command def-message def-failure]]
            [juncture.entity
             :as entity
             :refer [transform transform-in]]
            [componad
             :refer [within]]
            [vebento.core
             :refer [boundary publish fail-with get-entity]]))


(ns-alias 'product 'vebento.product)
(ns-alias 'merchant 'vebento.merchant)
(ns-alias 'customer 'vebento.customer)


(def-command ::customer/add-item-to-cart
  :req [::customer/id
        ::product/id
        ::product/amount])


(def-message ::customer/item-added-to-cart
  :req [::customer/id
        ::product/id
        ::product/amount])


(def-failure ::customer/product-not-in-merchant-assortment
  :req [::customer/id
        ::product/id])


(defmethod transform
  [::customer/entity ::customer/item-added-to-cart]
  [customer {product-id ::product/id amount ::product/amount}]
  (if (zero? amount)
    (update customer ::customer/cart dissoc product-id)
    (assoc-in customer [::customer/cart product-id] amount)))


(defn subscriptions
  [component]

  {::customer/item-added-to-cart
   [(transform-in (:repository component) ::customer/id)]

   ::customer/add-item-to-cart
   [(fn [{customer-id ::customer/id
          product-id ::product/id
          amount ::product/amount}]
      (within (boundary component #{::customer/shop})
        customer <- (get-entity ::customer/id customer-id)
        merchant <- (get-entity ::merchant/id (@customer ::merchant/id))
        (mwhen (-> product-id
                   (not-in? (@merchant ::merchant/products)))
               (fail-with ::customer/product-not-in-merchant-assortment
                          ::customer/id customer-id
                          ::product/id product-id))
        (publish ::customer/item-added-to-cart
                 ::customer/id customer-id
                 ::product/id product-id
                 ::product/amount amount)))]})
