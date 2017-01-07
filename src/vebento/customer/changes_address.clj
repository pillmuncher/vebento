(ns vebento.customer.changes-address
  (:require [clojure.future
             :refer :all]
            [util
             :refer [ns-alias]]
            [juncture.event
             :as event
             :refer [def-command def-message def-failure store-in]]
            [juncture.entity
             :as entity
             :refer [transform]]
            [componad
             :refer [within]]
            [vebento.core
             :refer [boundary publish fail-unless-exists transform-in]]))


(ns-alias 'customer 'vebento.customer)


(def-command ::customer/change-address
  :req [::customer/id
        ::customer/address])


(def-message ::customer/address-changed
  :req [::customer/id
        ::customer/address])


(def-failure ::customer/has-given-no-address
  :req [::customer/id])


(defmethod transform
  [::customer/entity ::customer/address-changed]
  [customer {address ::customer/address}]
  (assoc customer ::customer/address address))


(defn subscriptions
  [component]

  {::customer/change-address
   [(store-in (:journal component))
    (fn [{customer-id ::customer/id
          address ::customer/address}]
      (within (boundary component #{::customer/account ::customer/shopping})
        (fail-unless-exists ::customer/id customer-id)
        (publish ::customer/address-changed
                 ::customer/id customer-id
                 ::customer/address address)))]

   ::customer/address-changed
   [(store-in (:journal component))
    (transform-in (:entity-store component) ::customer/id)]

   ::customer/has-given-no-address
   [(store-in (:journal component))]})
