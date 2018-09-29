(ns vebento.customer.adds-schedule
  (:require [clojure.set
             :refer [union]]
            [monads.util
             :refer [mwhen]]
            [util
             :refer [ns-alias intersect?]]
            [juncture.event
             :as event
             :refer [def-command def-message def-failure]]
            [juncture.entity
             :as entity
             :refer [transform transform-in]]
            [componad
             :refer [mdo-within munless]]
            [vebento.core
             :refer [boundary publish raise get-entity]]))


(ns-alias 'merchant 'vebento.merchant)
(ns-alias 'customer 'vebento.customer)


(def-command ::customer/add-schedule
  :req [::customer/id
        ::customer/schedule])


(def-message ::customer/schedule-added
  :req [::customer/id
        ::customer/schedule])


(def-failure ::customer/schedule-not-in-merchant-schedule
  :req [::customer/id
        ::customer/schedule])


(defmethod transform
  [::customer/entity ::customer/schedule-added]
  [customer {schedule ::customer/schedule}]
  (update customer ::customer/schedule union schedule))


(defn subscriptions
  [component]

  {::customer/schedule-added
   [(transform-in (:repository component) ::customer/id)]

   ::customer/add-schedule
   [(fn [{customer-id ::customer/id
          schedule ::customer/schedule}]
      (mdo-within (boundary component #{::customer/shop})
        customer <- (get-entity ::customer/id customer-id)
        merchant <- (get-entity ::merchant/id (@customer ::merchant/id))
        (munless (intersect? schedule (@merchant ::merchant/schedule))
                 (raise ::customer/schedule-not-in-merchant-schedule
                        ::customer/id customer-id
                        ::customer/schedule schedule))
        (publish ::customer/schedule-added
                 ::customer/id customer-id
                 ::customer/schedule schedule)))]})
