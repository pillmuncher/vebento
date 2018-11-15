(ns vebento.merchant.adds-payment-method
  (:require [util
             :refer [ns-alias]]
            [juncture.event
             :as event
             :refer [defcommand defmessage deffailure]]
            [juncture.entity
             :as entity
             :refer [transform transform-in]]
            [componad
             :refer [mdo-within]]
            [vebento.core
             :refer [boundary publish fail-unless-exists]]))


(ns-alias 'merchant 'vebento.merchant)


(defcommand ::merchant/add-payment-method
  :req [::merchant/id
        ::merchant/payment-method])


(defmessage ::merchant/payment-method-added
  :req [::merchant/id
        ::merchant/payment-method])


(deffailure ::merchant/does-not-support-payment-method
  :req [::merchant/id
        ::merchant/payment-method])


(defmethod transform
  [::merchant/entity ::merchant/payment-method-added]
  [merchant {payment-method ::merchant/payment-method}]
  (update merchant ::merchant/payment-methods conj payment-method))


(defn subscriptions
  [component]

  {::merchant/payment-method-added
   [(transform-in (:repository component) ::merchant/id)]

   ::merchant/add-payment-method
   [(fn [{merchant-id ::merchant/id payment-method ::merchant/payment-method}]
      (mdo-within (boundary component #{::merchant/account})
        (fail-unless-exists ::merchant/id merchant-id)
        (publish ::merchant/payment-method-added
                 ::merchant/id merchant-id
                 ::merchant/payment-method payment-method)))]})
