(ns vebento.merchant.adds-area
  (:require [util
             :refer [ns-alias]]
            [juncture.event
             :as event
             :refer [defcommand defmessage]]
            [juncture.entity
             :as entity
             :refer [promote promote-in]]
            [componad
             :refer [mdo-within]]
            [vebento.core
             :refer [boundary post fail-unless-exists]]))


(ns-alias 'merchant 'vebento.merchant)


(defcommand ::merchant/add-area
  :req [::merchant/id
        ::merchant/zipcode])


(defmessage ::merchant/area-added
  :req [::merchant/id
        ::merchant/zipcode])


(defmethod promote
  [::merchant/entity ::merchant/area-added]
  [merchant {zipcode ::merchant/zipcode}]
  (update merchant ::merchant/areas conj zipcode))


(defn subscriptions
  [component]

  {::merchant/area-added
   [(promote-in (:repository component) ::merchant/id)]

   ::merchant/add-area
   [(fn [{merchant-id ::merchant/id zipcode ::merchant/zipcode}]
      (mdo-within (boundary component #{::merchant/account})
        (fail-unless-exists ::merchant/id merchant-id)
        (post ::merchant/area-added
                 ::merchant/id merchant-id
                 ::merchant/zipcode zipcode)))]})
