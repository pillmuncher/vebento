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
             :refer [def-scenario]]
            [vebento.customer-test
             :refer [test-bench]]))


(ns-alias 'customer 'vebento.customer)


(def-scenario customer-adds-address
  [customer-id ::customer/id
   customer-address ::customer/address]
  :using (test-bench)
  :given [(command
            ::customer/register
            ::customer/id customer-id)]
  :after [(command
            ::customer/change-address
            ::customer/id customer-id
            ::customer/address customer-address)]
  :reply [(message
            ::customer/address-changed
            ::customer/id customer-id
            ::customer/address customer-address)])


(def-scenario customer-changes-address
  [customer-id ::customer/id
   old-address ::customer/address
   new-address ::customer/address]
  :using (test-bench)
  :given [(command
            ::customer/register
            ::customer/id customer-id
            ::customer/address old-address)]
  :after [(command
            ::customer/change-address
            ::customer/id customer-id
            ::customer/address new-address)]
  :reply [(message
            ::customer/address-changed
            ::customer/id customer-id
            ::customer/address new-address)])
