(ns vebento.merchant.registers
  (:require [util
             :refer [ns-alias]]
            [juncture.event
             :as event
             :refer [def-command def-notice]]
            [juncture.entity
             :as entity
             :refer [create transform transform-in]]
            [componad
             :refer [mdo-within]]
            [vebento.core
             :refer [boundary publish fail-if-exists]]))


(ns-alias 'merchant 'vebento.merchant)


(def-command ::merchant/register
  :req [::merchant/id
        ::merchant/address])


(def-notice ::merchant/registered
  :req [::merchant/id
        ::merchant/address])


(defmethod transform
  [nil ::merchant/registered]
  [_   {merchant-id ::merchant/id address ::merchant/address}]
  (create ::merchant/entity
          ::entity/id merchant-id
          ::merchant/address address
          ::merchant/areas #{}
          ::merchant/products #{}
          ::merchant/schedule #{}
          ::merchant/payment-methods #{}
          ::merchant/customers #{}
          ::merchant/pending-orders #{}))


(defn subscriptions
  [component]

  {::merchant/registered
   [(transform-in (:repository component) ::merchant/id)]

   ::merchant/register
   [(fn [{merchant-id ::merchant/id address ::merchant/address}]
      (mdo-within (boundary component #{::merchant/account})
        (fail-if-exists ::merchant/id merchant-id)
        (publish ::merchant/registered
                 ::merchant/id merchant-id
                 ::merchant/address address)))]})
