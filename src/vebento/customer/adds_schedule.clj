(ns vebento.customer.adds-schedule
  (:require [clojure.future
             :refer :all]
            [clojure.set
             :refer [union intersection]]
            [monads.util
             :refer [mwhen]]
            [util
             :refer [ns-alias]]
            [juncture.event
             :as event
             :refer [def-command def-message def-failure]]
            [juncture.entity
             :as entity
             :refer [transform transform-in]]
            [componad
             :refer [within]]
            [vebento.core
             :refer [boundary publish fail-with get-entity]]))


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
      (within (boundary component #{::customer/shopping})
        customer <- (get-entity ::customer/id customer-id)
        merchant <- (get-entity ::merchant/id (@customer ::merchant/id))
        (mwhen (empty? (intersection schedule
                                     (@merchant ::merchant/schedule)))
               (fail-with ::customer/schedule-not-in-merchant-schedule
                          ::customer/id customer-id
                          ::customer/schedule schedule))
        (publish ::customer/schedule-added
                 ::customer/id customer-id
                 ::customer/schedule schedule)))]})
