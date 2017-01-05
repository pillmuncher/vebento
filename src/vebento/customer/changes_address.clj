(ns vebento.customer.changes-address
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

  [[::event/type ::customer/address-changed
    (transform-in (:entity-store component) ::customer/id)]

   [::event/type ::customer/change-address
    (fn [{customer-id ::customer/id
          address ::customer/address}]
      (within (boundary component #{::customer/account ::customer/shopping})
        (fail-unless-exists ::customer/id customer-id)
        (publish ::customer/address-changed
                 ::customer/id customer-id
                 ::customer/address address)))]])
