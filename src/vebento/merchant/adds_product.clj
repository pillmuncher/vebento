(ns vebento.merchant.adds-product
  (:require [util
             :refer [ns-alias]]
            [juncture.event
             :as event
             :refer [def-command def-notice]]
            [juncture.entity
             :as entity
             :refer [transform transform-in]]
            [componad
             :refer [mdo-within]]
            [vebento.core
             :refer [boundary publish fail-unless-exists]]))


(ns-alias 'merchant 'vebento.merchant)
(ns-alias 'product 'vebento.product)


(def-command ::merchant/add-product
  :req [::merchant/id
        ::product/id])


(def-notice ::merchant/product-added
  :req [::merchant/id
        ::product/id])


(defmethod transform
  [::merchant/entity ::merchant/product-added]
  [merchant {product-id ::product/id}]
  (update merchant ::merchant/products conj product-id))


(defn subscriptions
  [component]

  {::merchant/product-added
   [(transform-in (:repository component) ::merchant/id)]

   ::merchant/add-product
   [(fn [{merchant-id ::merchant/id product-id ::product/id}]
      (mdo-within (boundary component #{::merchant/account})
        (fail-unless-exists ::merchant/id merchant-id)
        (fail-unless-exists ::product/id product-id)
        (publish ::merchant/product-added
                 ::merchant/id merchant-id
                 ::product/id product-id)))]})
