(ns vebento.customer.selects-merchant
  (:require [monads.util
             :refer [mwhen]]
            [util
             :refer [ns-alias not-in?]]
            [juncture.event
             :as event
             :refer [defcommand defmessage deffailure message failure]]
            [juncture.entity
             :as entity
             :refer [mutate mutate-in]]
            [componad
             :refer [mdo-within]]
            [vebento.core
             :refer [boundary get-entity call post fail]]))


(ns-alias 'specs 'vebento.specs)
(ns-alias 'merchant 'vebento.merchant)
(ns-alias 'customer 'vebento.customer)


(defcommand ::customer/select-merchant
  :req [::customer/id
        ::merchant/id])


(defmessage ::customer/merchant-selected
  :req [::customer/id
        ::merchant/id])


(deffailure ::customer/zipcode-not-in-merchant-areas
  :req [::customer/id
        ::customer/zipcode])


(defmethod mutate
  [::customer/entity ::customer/merchant-selected]
  [customer {merchant-id ::merchant/id}]
  (assoc customer ::merchant/id merchant-id))


(defn subscriptions
  [component]

  {::customer/merchant-selected
   [(mutate-in (:repository component) ::customer/id)]

   ::customer/select-merchant
   [(fn [{customer-id ::customer/id
          merchant-id ::merchant/id}]
      (mdo-within (boundary component #{::customer/shop})
        customer <- (get-entity ::customer/id customer-id)
        (mwhen (-> @customer ::customer/address nil?)
               (fail ::customer/has-given-no-address
                     ::customer/id customer-id))
        merchant <- (get-entity ::merchant/id merchant-id)
        (mwhen (-> @customer ::customer/address ::specs/zipcode
                   (not-in? (@merchant ::merchant/areas)))
               (fail ::customer/zipcode-not-in-merchant-areas
                     ::customer/id customer-id
                     ::customer/zipcode (-> @customer
                                            ::customer/address
                                            ::specs/zipcode)))
        (post ::customer/merchant-selected
              ::customer/id customer-id
              ::merchant/id merchant-id)))]})
