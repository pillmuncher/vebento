(ns vebento.merchant.registers
  (:require [clojure.future
             :refer :all]
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
             :refer [boundary publish fail-if-exists transform-in]]))


(ns-alias 'merchant 'vebento.merchant)


(def-command ::merchant/register
  :req [::merchant/id
        ::merchant/address])


(def-message ::merchant/registered
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
   [(transform-in (:entity-store component) ::merchant/id)]

   ::merchant/register
   [(fn [{merchant-id ::merchant/id address ::merchant/address}]
      (within (boundary component #{::merchant/account})
        (fail-if-exists ::merchant/id merchant-id)
        (publish ::merchant/registered
                 ::merchant/id merchant-id
                 ::merchant/address address)))]})
