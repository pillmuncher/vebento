(ns vebento.merchant.adds-payment-method
  (:require [clojure.future
             :refer :all]
            [util
             :refer [ns-alias]]
            [juncture.event
             :as event
             :refer [def-command def-message def-failure]]
            [juncture.entity
             :as entity
             :refer [transform]]
            [componad
             :refer [within]]
            [vebento.core
             :refer [boundary publish fail-unless-exists transform-in]]))


(ns-alias 'merchant 'vebento.merchant)



(def-command ::merchant/add-payment-method
  :req [::merchant/id
        ::merchant/payment-method])


(def-message ::merchant/payment-method-added
  :req [::merchant/id
        ::merchant/payment-method])


(def-failure ::merchant/does-not-support-payment-method
  :req [::merchant/id
        ::merchant/payment-method])


(defmethod transform
  [::merchant/entity ::merchant/payment-method-added]
  [merchant {payment-method ::merchant/payment-method}]
  (update merchant ::merchant/payment-methods conj payment-method))

(defn subscriptions
  [component]

  [[::event/type ::merchant/payment-method-added
    (transform-in (:entity-store component) ::merchant/id)]

   [::event/type ::merchant/add-payment-method
    (fn [{merchant-id ::merchant/id payment-method ::merchant/payment-method}]
      (within (boundary component [::merchant/account] merchant-id)
        (fail-unless-exists ::merchant/id merchant-id)
        (publish ::merchant/payment-method-added
                 ::merchant/id merchant-id
                 ::merchant/payment-method payment-method)))]])
