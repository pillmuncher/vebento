(ns vebento.customer.registers
  (:require [monads.core
             :as monad
             :refer [return]]
            [monads.util
             :refer [mwhen]]
            [util
             :refer [ns-alias]]
            [juncture.event
             :as event
             :refer [defcommand defmessage command message]]
            [juncture.entity
             :as entity
             :refer [create transform transform-in]]
            [componad
             :refer [mdo-within >>=]]
            [vebento.core
             :refer [boundary issue fail-if-exists]]))


(ns-alias 'merchant 'vebento.merchant)
(ns-alias 'customer 'vebento.customer)


(defcommand ::customer/register
  :req [::customer/id]
  :opt [::customer/address
        ::merchant/id
        ::customer/payment-method])


(defmessage ::customer/registered
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
   [(transform-in (:repository component) ::customer/id)]

   ::customer/register
   [(fn [{customer-id ::customer/id
          address ::customer/address
          merchant-id ::merchant/id
          payment-method ::customer/payment-method}]
      (mdo-within (boundary component #{::customer/account ::customer/shop})
        (fail-if-exists ::customer/id customer-id)
        (issue
          (message ::customer/registered
                   ::customer/id customer-id))
        (mwhen (some? address)
               (issue
                 (command ::customer/change-address
                          ::customer/id customer-id
                          ::customer/address address)))
        (mwhen (some? merchant-id)
               (issue
                 (command ::customer/select-merchant
                          ::customer/id customer-id
                          ::merchant/id merchant-id)))
        (mwhen (some? payment-method)
               (issue
                 (command ::customer/select-payment-method
                          ::customer/id customer-id
                          ::customer/payment-method payment-method)))))]})
