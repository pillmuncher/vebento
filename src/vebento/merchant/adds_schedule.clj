(ns vebento.merchant.adds-schedule
  (:require [clojure.future
             :refer :all]
            [clojure.set
             :refer [union]]
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
             :refer [aggregate publish fail-unless-exists transform-in]]))


(ns-alias 'merchant 'vebento.merchant)


(def-command ::merchant/add-schedule
  :req [::merchant/id
        ::merchant/schedule])

(def-message ::merchant/schedule-added
  :req [::merchant/id
        ::merchant/schedule])

(defmethod transform
  [::merchant/entity ::merchant/schedule-added]
  [merchant {schedule ::merchant/schedule}]
  (update merchant ::merchant/schedule union schedule))

(defn subscriptions
  [component]

        [[::event/type ::merchant/schedule-added
         (transform-in (:entity-store component) ::merchant/id)]

        [::event/type ::merchant/add-schedule
         (fn [{merchant-id ::merchant/id schedule ::merchant/schedule}]
           (within (aggregate component [::merchant/account] merchant-id)
             (fail-unless-exists ::merchant/id merchant-id)
             (publish ::merchant/schedule-added
                      ::merchant/id merchant-id
                      ::merchant/schedule schedule)))]])
