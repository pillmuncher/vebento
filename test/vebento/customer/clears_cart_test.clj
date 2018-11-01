(ns vebento.customer.clears-cart-test
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
             :refer [test-environment]]))


(ns-alias 'customer 'vebento.customer)


(def-scenario customer-clears-cart
  [customer-id ::customer/id]
  :using (test-environment)
  :given [(command
            ::customer/register
            ::customer/id customer-id)]
  :after [(command
            ::customer/clear-cart
            ::customer/id customer-id)]
  :issue [(message
            ::customer/cart-cleared
            ::customer/id customer-id)])


(def-scenario only-an-existing-customer-can-clear-cart
  [customer-id ::customer/id]
  :using (test-environment)
  :after [(command
            ::customer/clear-cart
            ::customer/id customer-id)]
  :issue [(failure
            ::entity/not-found
            ::entity/id-key ::customer/id
            ::entity/id customer-id)])
