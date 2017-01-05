(ns vebento.merchant.adds-area
  (:require [clojure.future
             :refer :all]
            [util
             :refer [ns-alias]]
            [juncture.event
             :as event
             :refer [def-command def-message]]
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

  [[::event/type ::merchant/area-added
    (transform-in (:entity-store component) ::merchant/id)]

   [::event/type ::merchant/add-area
    (fn [{merchant-id ::merchant/id zipcode ::merchant/zipcode}]
      (within (boundary component [::merchant/account] merchant-id)
        (fail-unless-exists ::merchant/id merchant-id)
        (publish ::merchant/area-added
                 ::merchant/id merchant-id
                 ::merchant/zipcode zipcode)))]])
