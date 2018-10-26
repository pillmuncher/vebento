(ns vebento.customer.registers-test
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
(ns-alias 'order 'vebento.order)
(ns-alias 'merchant 'vebento.merchant)
(ns-alias 'customer 'vebento.customer)


(def-scenario customer-registers
  [customer-id ::customer/id]
  :using (test-bench)
  :after [(command
            ::customer/register
            ::customer/id customer-id)]
  :issue [(message
            ::customer/registered
            ::customer/id customer-id)])


(def-scenario customer-can-register-only-once
  [customer-id ::customer/id]
  :using (test-bench)
  :given [(command
            ::customer/register
            ::customer/id customer-id)]
  :after [(command
            ::customer/register
            ::customer/id customer-id)]
  :issue [(failure
            ::entity/already-exists
            ::entity/id-key ::customer/id
            ::entity/id customer-id)])


(def-scenario customer-gets-registered-with-address
  [customer-id ::customer/id
   customer-address ::customer/address]
  :using (test-bench)
  :after [(command
            ::customer/register
            ::customer/id customer-id
            ::customer/address customer-address)]
  :issue [(message
            ::customer/registered
            ::customer/id customer-id)
          (message
            ::customer/address-changed
            ::customer/id customer-id
            ::customer/address customer-address)])


(def-scenario customer-gets-registered-with-merchant
  [customer-id ::customer/id
   customer-address ::customer/address
   merchant-id ::merchant/id
   merchant-address ::merchant/address]
  :using (test-bench)
  :given [(command
            ::merchant/register
            ::merchant/id merchant-id
            ::merchant/address merchant-address)
          (command
            ::merchant/add-area
            ::merchant/id merchant-id
            ::merchant/zipcode (::specs/zipcode customer-address))]
  :after [(command
            ::customer/register
            ::customer/id customer-id
            ::customer/address customer-address
            ::merchant/id merchant-id)]
  :issue [(message
            ::customer/registered
            ::customer/id customer-id)
          (message
            ::customer/address-changed
            ::customer/id customer-id
            ::customer/address customer-address)
          (message
            ::customer/merchant-selected
            ::customer/id customer-id
            ::merchant/id merchant-id)])


(def-scenario customer-gets-registered-with-payment-method
  [customer-id ::customer/id
   customer-address ::customer/address
   merchant-id ::merchant/id
   merchant-address ::merchant/address
   payment-method ::order/payment-method]
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
            ::merchant/add-payment-method
            ::merchant/id merchant-id
            ::merchant/payment-method payment-method)]
  :after [(command
            ::customer/register
            ::customer/id customer-id
            ::customer/address customer-address
            ::merchant/id merchant-id
            ::customer/payment-method payment-method)]
  :issue [(message
            ::customer/registered
            ::customer/id customer-id)
          (message
            ::customer/address-changed
            ::customer/id customer-id
            ::customer/address customer-address)
          (message
            ::customer/merchant-selected
            ::customer/id customer-id
            ::merchant/id merchant-id)
          (message
            ::customer/payment-method-selected
            ::customer/id customer-id
            ::customer/payment-method payment-method)])


(def-scenario customer-registers-but-cannot-select-retailer
  [customer-id ::customer/id
   customer-address ::customer/address
   merchant-id ::merchant/id]
  :using (test-bench)
  :after [(command
            ::customer/register
            ::customer/id customer-id
            ::customer/address customer-address
            ::merchant/id merchant-id)]
  :issue [(message
            ::customer/registered
            ::customer/id customer-id)
          (message
            ::customer/address-changed
            ::customer/id customer-id
            ::customer/address customer-address)
          (failure
            ::entity/not-found
            ::entity/id-key ::merchant/id
            ::entity/id merchant-id)])
