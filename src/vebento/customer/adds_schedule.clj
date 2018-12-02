(ns vebento.customer.adds-schedule
  (:require [clojure.set
             :refer [union]]
            [monads.util
             :refer [mwhen]]
            [util
             :refer [ns-alias intersect?]]
            [juncture.event
             :as event
             :refer [defcommand defmessage deffailure message failure]]
            [juncture.entity
             :as entity
             :refer [mutate mutate-in]]
            [componad
             :refer [mdo-within munless]]
            [vebento.core
             :refer [boundary get-entity call post fail]]))


(ns-alias 'merchant 'vebento.merchant)
(ns-alias 'customer 'vebento.customer)


(defcommand ::customer/add-schedule
  :req [::customer/id
        ::customer/schedule])


(defmessage ::customer/schedule-added
  :req [::customer/id
        ::customer/schedule])


(deffailure ::customer/schedule-not-in-merchant-schedule
  :req [::customer/id
        ::customer/schedule])


(defmethod mutate
  [::customer/entity ::customer/schedule-added]
  [customer {schedule ::customer/schedule}]
  (update customer ::customer/schedule union schedule))


(defn subscriptions
  [component]

  {::customer/schedule-added
   [(mutate-in (:repository component) ::customer/id)]

   ::customer/add-schedule
   [(fn [{customer-id ::customer/id
          schedule ::customer/schedule}]
      (mdo-within (boundary component #{::customer/shop})
        customer <- (get-entity ::customer/id customer-id)
        merchant <- (get-entity ::merchant/id (@customer ::merchant/id))
        (munless (intersect? schedule (@merchant ::merchant/schedule))
                 (fail ::customer/schedule-not-in-merchant-schedule
                       ::customer/id customer-id
                       ::customer/schedule schedule))
        (post ::customer/schedule-added
              ::customer/id customer-id
              ::customer/schedule schedule)))]})
