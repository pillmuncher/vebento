(ns vebento.customer.adds-item-to-cart
  (:require [monads.util
             :refer [mwhen]]
            [util
             :refer [ns-alias not-in?]]
            [juncture.event
             :as event
             :refer [defcommand defmessage deffailure message failure]]
            [juncture.entity
             :as entity
             :refer [transform transform-in]]
            [componad
             :refer [mdo-within]]
            [vebento.core
             :refer [boundary issue get-entity]]))


(ns-alias 'product 'vebento.product)
(ns-alias 'merchant 'vebento.merchant)
(ns-alias 'customer 'vebento.customer)


(defcommand ::customer/add-item-to-cart
  :req [::customer/id
        ::product/id
        ::product/amount])


(defmessage ::customer/item-added-to-cart
  :req [::customer/id
        ::product/id
        ::product/amount])


(deffailure ::customer/product-not-in-merchant-assortment
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
      (mdo-within (boundary component #{::customer/shop})
        customer <- (get-entity ::customer/id customer-id)
        merchant <- (get-entity ::merchant/id (@customer ::merchant/id))
        (mwhen (-> product-id
                   (not-in? (@merchant ::merchant/products)))
               (issue
                 (failure ::customer/product-not-in-merchant-assortment
                          ::customer/id customer-id
                          ::product/id product-id)))
        (issue
          (message ::customer/item-added-to-cart
                   ::customer/id customer-id
                   ::product/id product-id
                   ::product/amount amount))))]})
