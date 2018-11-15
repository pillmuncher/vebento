(ns vebento.customer.changes-address-test
  (:require [clojure.test
             :refer :all]
            [util
             :refer [ns-alias]]
            [juncture.event
             :refer [command message failure]]
            [juncture.entity
             :as entity]
            [vebento.testing
             :refer [defscenario]]
            [vebento.customer-test
             :refer [test-environment]]))


(ns-alias 'customer 'vebento.customer)


(defscenario customer-adds-address
  [customer-id ::customer/id
   customer-address ::customer/address]
  :using (test-environment)
  :given [(command
            ::customer/register
            ::customer/id customer-id)]
  :after [(command
            ::customer/change-address
            ::customer/id customer-id
            ::customer/address customer-address)]
  :issue [(message
            ::customer/address-changed
            ::customer/id customer-id
            ::customer/address customer-address)])


(defscenario customer-changes-address
  [customer-id ::customer/id
   old-address ::customer/address
   new-address ::customer/address]
  :using (test-environment)
  :given [(command
            ::customer/register
            ::customer/id customer-id
            ::customer/address old-address)]
  :after [(command
            ::customer/change-address
            ::customer/id customer-id
            ::customer/address new-address)]
  :issue [(message
            ::customer/address-changed
            ::customer/id customer-id
            ::customer/address new-address)])
