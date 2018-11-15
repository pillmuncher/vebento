(ns vebento.merchant.adds-schedule
  (:require [clojure.set
             :refer [union]]
            [util
             :refer [ns-alias]]
            [juncture.event
             :as event
             :refer [defcommand defmessage]]
            [juncture.entity
             :as entity
             :refer [transform transform-in]]
            [componad
             :refer [mdo-within]]
            [vebento.core
             :refer [boundary publish fail-unless-exists]]))


(ns-alias 'merchant 'vebento.merchant)


(defcommand ::merchant/add-schedule
  :req [::merchant/id
        ::merchant/schedule])


(defmessage ::merchant/schedule-added
  :req [::merchant/id
        ::merchant/schedule])


(defmethod transform
  [::merchant/entity ::merchant/schedule-added]
  [merchant {schedule ::merchant/schedule}]
  (update merchant ::merchant/schedule union schedule))


(defn subscriptions
  [component]

  {::merchant/schedule-added
   [(transform-in (:repository component) ::merchant/id)]

   ::merchant/add-schedule
   [(fn [{merchant-id ::merchant/id schedule ::merchant/schedule}]
      (mdo-within (boundary component #{::merchant/account})
        (fail-unless-exists ::merchant/id merchant-id)
        (publish ::merchant/schedule-added
                 ::merchant/id merchant-id
                 ::merchant/schedule schedule)))]})
