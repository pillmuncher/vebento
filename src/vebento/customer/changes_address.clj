(ns vebento.customer.changes-address
  (:require [util
             :refer [ns-alias]]
            [juncture.event
             :as event
             :refer [def-command def-message def-failure message]]
            [juncture.entity
             :as entity
             :refer [transform transform-in]]
            [componad
             :refer [mdo-within]]
            [vebento.core
             :refer [boundary issue fail-unless-exists]]))


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

  {::customer/address-changed
   [(transform-in (:repository component) ::customer/id)]

   ::customer/change-address
   [(fn [{customer-id ::customer/id
          address ::customer/address}]
      (mdo-within (boundary component #{::customer/account ::customer/shop})
        (fail-unless-exists ::customer/id customer-id)
        (issue
          (message ::customer/address-changed
                   ::customer/id customer-id
                   ::customer/address address))))]})
