(ns vebento.merchant.adds-area
  (:require [util
             :refer [ns-alias]]
            [juncture.event
             :as event
             :refer [def-command def-message]]
            [juncture.entity
             :as entity
             :refer [transform transform-in]]
            [componad
             :refer [mdo-within]]
            [vebento.core
             :refer [boundary publish fail-unless-exists]]))


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

  {::merchant/area-added
   [(transform-in (:repository component) ::merchant/id)]

   ::merchant/add-area
   [(fn [{merchant-id ::merchant/id zipcode ::merchant/zipcode}]
      (mdo-within (boundary component #{::merchant/account})
        (fail-unless-exists ::merchant/id merchant-id)
        (publish ::merchant/area-added
                 ::merchant/id merchant-id
                 ::merchant/zipcode zipcode)))]})
