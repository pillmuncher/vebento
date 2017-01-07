(ns vebento.merchant.adds-area
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


(def-command ::merchant/add-area
  :req [::merchant/id
        ::merchant/zipcode])


(def-message ::merchant/area-added
  :req [::merchant/id
        ::merchant/zipcode])


(defmethod transform
  [::merchant/entity ::merchant/area-added]
  [merchant {zipcode ::merchant/zipcode}]
  (update merchant ::merchant/areas conj zipcode))


(defn subscriptions
  [component]

  {::merchant/add-area
   [(store-in (:journal component))
    (fn [{merchant-id ::merchant/id zipcode ::merchant/zipcode}]
      (within (boundary component #{::merchant/account})
        (fail-unless-exists ::merchant/id merchant-id)
        (publish ::merchant/area-added
                 ::merchant/id merchant-id
                 ::merchant/zipcode zipcode)))]

   ::merchant/area-added
   [(store-in (:journal component))
    (transform-in (:entity-store component) ::merchant/id)]})
