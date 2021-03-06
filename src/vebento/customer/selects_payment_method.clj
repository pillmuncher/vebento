(ns vebento.customer.selects-payment-method
  (:require [monads.core
             :refer [return]]
            [monads.util
             :refer [mwhen]]
            [util
             :refer [ns-alias not-in?]]
            [juncture.event
             :as event
             :refer [defcommand defmessage message failure]]
            [juncture.entity
             :as entity
             :refer [mutate mutate-in]]
            [componad
             :refer [mdo-within]]
            [vebento.core
             :refer [boundary get-entity call post fail]]))


(ns-alias 'merchant 'vebento.merchant)
(ns-alias 'customer 'vebento.customer)


(defcommand ::customer/select-payment-method
  :req [::customer/id
        ::customer/payment-method])


(defmessage ::customer/payment-method-selected
  :req [::customer/id
        ::customer/payment-method])


(defmethod mutate
  [::customer/entity ::customer/payment-method-selected]
  [customer {payment-method ::customer/payment-method}]
  (assoc customer ::customer/payment-method payment-method))


(defn subscriptions
  [component]

  {::customer/payment-method-selected
   [(mutate-in (:repository component) ::customer/id)]

   ::customer/select-payment-method
   [(fn [{customer-id ::customer/id
          payment-method ::customer/payment-method}]
      (mdo-within (boundary component #{::customer/shop})
        customer <- (get-entity ::customer/id customer-id)
        merchant <- (get-entity ::merchant/id (@customer ::merchant/id))
        (mwhen (-> payment-method
                   (not-in? (@merchant ::merchant/payment-methods)))
               (fail ::merchant/does-not-support-payment-method
                     ::merchant/id (@merchant ::entity/id)
                     ::merchant/payment-method payment-method))
        (post ::customer/payment-method-selected
              ::customer/id customer-id
              ::customer/payment-method payment-method)))]})
