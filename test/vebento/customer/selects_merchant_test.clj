(ns vebento.customer.selects-merchant-test
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


(ns-alias 'specs 'vebento.specs)
(ns-alias 'merchant 'vebento.merchant)
(ns-alias 'customer 'vebento.customer)


(def-scenario customer-selects-merchant
  [customer-id ::customer/id
   merchant-id ::merchant/id
   customer-address ::customer/address
   merchant-address ::merchant/address]
  :using (test-bench)
  :given [(command
            ::merchant/register
            ::merchant/id merchant-id
            ::merchant/address merchant-address)
          (command
            ::merchant/add-area
            ::merchant/id merchant-id
            ::merchant/zipcode (::specs/zipcode customer-address))
          (command
            ::customer/register
            ::customer/id customer-id
            ::customer/address customer-address)]
  :after [(command
            ::customer/select-merchant
            ::customer/id customer-id
            ::merchant/id merchant-id)]
  :issue [(message
            ::customer/merchant-selected
            ::customer/id customer-id
            ::merchant/id merchant-id)])


(def-scenario only-an-existing-customer-can-select-merchant
  [customer-id ::customer/id
   merchant-id ::merchant/id]
  :using (test-bench)
  :after [(command
            ::customer/select-merchant
            ::customer/id customer-id
            ::merchant/id merchant-id)]
  :issue [(failure
            ::entity/not-found
            ::entity/id-key ::customer/id
            ::entity/id customer-id)])


(def-scenario customer-cannot-select-merchant-unless-customer-address-was-given
  [customer-id ::customer/id
   merchant-id ::merchant/id
   customer-address ::customer/address
   merchant-address ::merchant/address]
  :using (test-bench)
  :given [(command
            ::merchant/register
            ::merchant/id merchant-id
            ::merchant/address merchant-address)
          (command
            ::merchant/add-area
            ::merchant/id merchant-id
            ::merchant/zipcode (::specs/zipcode customer-address))
          (command
            ::customer/register
            ::customer/id customer-id)]
  :after [(command
            ::customer/select-merchant
            ::customer/id customer-id
            ::merchant/id merchant-id)]
  :issue [(failure
            ::customer/has-given-no-address
            ::customer/id customer-id)])


(def-scenario customer-can-only-select-merchant-who-delivers-in-customer-area
  [customer-id ::customer/id
   merchant-id ::merchant/id
   customer-address ::customer/address
   merchant-address ::merchant/address]
  :using (test-bench)
  :given [(command
            ::merchant/register
            ::merchant/id merchant-id
            ::merchant/address merchant-address)
          (command
            ::customer/register
            ::customer/id customer-id
            ::customer/address customer-address)]
  :after [(command
            ::customer/select-merchant
            ::customer/id customer-id
            ::merchant/id merchant-id)]
  :issue [(failure
            ::customer/zipcode-not-in-merchant-areas
            ::customer/id customer-id
            ::customer/zipcode (::specs/zipcode customer-address))])
