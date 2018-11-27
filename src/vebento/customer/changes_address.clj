(ns vebento.customer.changes-address
  (:require [util
             :refer [ns-alias]]
            [juncture.event
             :as event
             :refer [defcommand defmessage deffailure message]]
            [juncture.entity
             :as entity
             :refer [mutate mutate-in]]
            [componad
             :refer [mdo-within]]
            [vebento.core
             :refer [boundary fail-unless-exists call post fail]]))


(ns-alias 'customer 'vebento.customer)


(defcommand ::customer/change-address
  :req [::customer/id
        ::customer/address])


(defmessage ::customer/address-changed
  :req [::customer/id
        ::customer/address])


(deffailure ::customer/has-given-no-address
  :req [::customer/id])


(defmethod mutate
  [::customer/entity ::customer/address-changed]
  [customer {address ::customer/address}]
  (assoc customer ::customer/address address))


(defn subscriptions
  [component]

  {::customer/address-changed
   [(mutate-in (:repository component) ::customer/id)]

   ::customer/change-address
   [(fn [{customer-id ::customer/id
          address ::customer/address}]
      (mdo-within (boundary component #{::customer/account ::customer/shop})
        (fail-unless-exists ::customer/id customer-id)
        (post ::customer/address-changed
              ::customer/id customer-id
              ::customer/address address)))]})
