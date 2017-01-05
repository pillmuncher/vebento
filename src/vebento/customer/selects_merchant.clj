(ns vebento.customer.selects-merchant
  (:require [clojure.future
             :refer :all]
            [monads.util
             :refer [mwhen]]
            [util
             :refer [ns-alias not-in?]]
            [juncture.event
             :as event
             :refer [def-command def-message def-failure]]
            [juncture.entity
             :as entity
             :refer [transform]]
            [componad
             :refer [within]]
            [vebento.core
             :refer [boundary publish fail-with transform-in get-entity]]))


(ns-alias 'specs 'vebento.specs)
(ns-alias 'merchant 'vebento.merchant)
(ns-alias 'customer 'vebento.customer)


(def-command ::customer/select-merchant
  :req [::customer/id
        ::merchant/id])


(def-message ::customer/merchant-selected
  :req [::customer/id
        ::merchant/id])


(def-failure ::customer/zipcode-not-in-merchant-areas
  :req [::customer/id
        ::customer/zipcode])


(defmethod transform
  [::customer/entity ::customer/merchant-selected]
  [customer {merchant-id ::merchant/id}]
  (assoc customer ::merchant/id merchant-id))


(defn subscriptions
  [component]

  [[::event/type ::customer/merchant-selected
    (transform-in (:entity-store component) ::customer/id)]

   [::event/type ::customer/select-merchant
    (fn [{customer-id ::customer/id
          merchant-id ::merchant/id}]
      (within (boundary component #{::customer/shopping})
        customer <- (get-entity ::customer/id customer-id)
        (mwhen (-> @customer ::customer/address nil?)
               (fail-with ::customer/has-given-no-address
                          ::customer/id customer-id))
        merchant <- (get-entity ::merchant/id merchant-id)
        (mwhen (->> @customer ::customer/address ::specs/zipcode
                    (not-in? (@merchant ::merchant/areas)))
               (fail-with ::customer/zipcode-not-in-merchant-areas
                          ::customer/id customer-id
                          ::customer/zipcode (-> @customer
                                                 ::customer/address
                                                 ::specs/zipcode)))
        (publish ::customer/merchant-selected
                 ::customer/id customer-id
                 ::merchant/id merchant-id)))]])
