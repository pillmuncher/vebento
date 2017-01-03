(ns vebento.customer.selects-payment-method
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
             :refer [aggregate publish fail-with transform-in get-entity]]))


(ns-alias 'merchant 'vebento.merchant)
(ns-alias 'customer 'vebento.customer)


(def-command ::customer/select-payment-method
  :req [::customer/id
        ::customer/payment-method])


(def-message ::customer/payment-method-selected
  :req [::customer/id
        ::customer/payment-method])


(defmethod transform
  [::customer/entity ::customer/payment-method-selected]
  [customer {payment-method ::customer/payment-method}]
  (assoc customer ::customer/payment-method payment-method))


(defn subscriptions
  [component]

  [[::event/type ::customer/payment-method-selected
    (transform-in (:entity-store component) ::customer/id)]

   [::event/type ::customer/select-payment-method
    (fn [{customer-id ::customer/id
          payment-method ::customer/payment-method}]
      (within (aggregate component [::customer/shopping] customer-id)
        customer <- (get-entity ::customer/id customer-id)
        merchant <- (get-entity ::merchant/id (@customer ::merchant/id))
        (mwhen (->> payment-method
                    (not-in? (@merchant ::merchant/payment-methods)))
               (fail-with ::merchant/does-not-support-payment-method
                          ::merchant/id (@merchant ::merchant/id)
                          ::merchant/payment-method payment-method))
        (publish ::customer/payment-method-selected
                 ::customer/id customer-id
                 ::customer/payment-method payment-method)))]])