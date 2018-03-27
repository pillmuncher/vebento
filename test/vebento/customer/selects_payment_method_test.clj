(ns vebento.customer.selects-payment-method-test
  (:require [clojure.test
             :refer :all]
            [util
             :refer [ns-alias]]
            [juncture.event
             :refer [command message error]]
            [vebento.testing
             :refer [def-scenario]]
            [vebento.customer-test
             :refer [test-bench]]))


(ns-alias 'specs 'vebento.specs)
(ns-alias 'order 'vebento.order)
(ns-alias 'merchant 'vebento.merchant)
(ns-alias 'customer 'vebento.customer)


(def-scenario customer-selects-payment-method
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
            ::merchant/payment-method payment-method)
          (command
            ::customer/register
            ::customer/id customer-id
            ::customer/address customer-address
            ::merchant/id merchant-id)]
  :after [(command
            ::customer/select-payment-method
            ::customer/id customer-id
            ::customer/payment-method payment-method)]
  :relay [(message
            ::customer/payment-method-selected
            ::customer/id customer-id
            ::customer/payment-method payment-method)])


(def-scenario customer-can-only-select-payment-method-that-merchant-supports
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
            ::customer/register
            ::customer/id customer-id
            ::customer/address customer-address
            ::merchant/id merchant-id)]
  :after [(command
            ::customer/select-payment-method
            ::customer/id customer-id
            ::customer/payment-method payment-method)]
  :relay [(error
            ::merchant/does-not-support-payment-method
            ::merchant/id merchant-id
            ::merchant/payment-method payment-method)])
