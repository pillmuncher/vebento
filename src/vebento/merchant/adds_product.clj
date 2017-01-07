(ns vebento.merchant.adds-product
  (:require [clojure.future
             :refer :all]
            [util
             :refer [ns-alias]]
            [juncture.event
             :as event
             :refer [def-command def-message store-in]]
            [juncture.entity
             :as entity
             :refer [transform]]
            [componad
             :refer [within]]
            [vebento.core
             :refer [boundary publish fail-unless-exists transform-in]]))


(ns-alias 'merchant 'vebento.merchant)
(ns-alias 'product 'vebento.product)


(def-command ::merchant/add-product
  :req [::merchant/id
        ::product/id])


(def-message ::merchant/product-added
  :req [::merchant/id
        ::product/id])


(defmethod transform
  [::merchant/entity ::merchant/product-added]
  [merchant {product-id ::product/id}]
  (update merchant ::merchant/products conj product-id))


(defn subscriptions
  [component]

  {::merchant/add-product
   [(store-in (:journal component))
    (fn [{merchant-id ::merchant/id product-id ::product/id}]
      (within (boundary component #{::merchant/account})
        (fail-unless-exists ::merchant/id merchant-id)
        (fail-unless-exists ::product/id product-id)
        (publish ::merchant/product-added
                 ::merchant/id merchant-id
                 ::product/id product-id)))]

   ::merchant/product-added
   [(store-in (:journal component))
    (transform-in (:entity-store component) ::merchant/id)]})
