(ns vebento.customer.registers
  (:require [clojure.future
             :refer :all]
            [monads.util
             :refer [mwhen]]
            [util
             :refer [ns-alias]]
            [juncture.event
             :as event
             :refer [def-command def-message]]
            [juncture.entity
             :as entity
             :refer [create transform]]
            [componad
             :refer [within]]
            [vebento.core
             :refer [boundary publish execute fail-if-exists transform-in]]))


(ns-alias 'merchant 'vebento.merchant)
(ns-alias 'customer 'vebento.customer)


(def-command ::customer/register
  :req [::customer/id]
  :opt [::customer/address
        ::merchant/id
        ::customer/payment-method])


(def-message ::customer/registered
  :req [::customer/id])


(defmethod transform
  [nil ::customer/registered]
  [_   {customer-id ::customer/id}]
  (create ::customer/entity
          ::entity/id customer-id
          ::customer/cart {}
          ::customer/schedule #{}
          ::customer/pending-orders #{}))


(defn subscriptions
  [component]

  {::customer/registered
   [(transform-in (:entity-store component) ::customer/id)]

   ::customer/register
   [(fn [{customer-id ::customer/id
          address ::customer/address
          merchant-id ::merchant/id
          payment-method ::customer/payment-method}]
      (within (boundary component #{::customer/account ::customer/shopping})
        (fail-if-exists ::customer/id customer-id)
        (publish ::customer/registered
                 ::customer/id customer-id)
        (mwhen (some? address)
               (execute ::customer/change-address
                        ::customer/id customer-id
                        ::customer/address address))
        (mwhen (some? merchant-id)
               (execute ::customer/select-merchant
                        ::customer/id customer-id
                        ::merchant/id merchant-id))
        (mwhen (some? payment-method)
               (execute ::customer/select-payment-method
                        ::customer/id customer-id
                        ::customer/payment-method payment-method))))]})
