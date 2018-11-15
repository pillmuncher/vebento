(ns vebento.customer.adds-schedule-test
  (:require [clojure.test
             :refer :all]
            [util
             :refer [ns-alias]]
            [juncture.event
             :refer [command message failure]]
            [vebento.testing
             :refer [defscenario]]
            [vebento.customer-test
             :refer [test-environment]]))


(ns-alias 'specs 'vebento.specs)
(ns-alias 'order 'vebento.order)
(ns-alias 'merchant 'vebento.merchant)
(ns-alias 'customer 'vebento.customer)


(defscenario customer-adds-schedule
  [customer-id ::customer/id
   customer-address ::customer/address
   merchant-id ::merchant/id
   merchant-address ::merchant/address
   schedule ::order/schedule]
  :using (test-environment)
  :given [(command
            ::merchant/register
            ::merchant/id merchant-id
            ::merchant/address merchant-address)
          (command
            ::merchant/add-area
            ::merchant/id merchant-id
            ::merchant/zipcode (::specs/zipcode customer-address))
          (command
            ::merchant/add-schedule
            ::merchant/id merchant-id
            ::merchant/schedule schedule)
          (command
            ::customer/register
            ::customer/id customer-id
            ::customer/address customer-address
            ::merchant/id merchant-id)]
  :after [(command
            ::customer/add-schedule
            ::customer/id customer-id
            ::customer/schedule schedule)]
  :issue [(message
            ::customer/schedule-added
            ::customer/id customer-id
            ::customer/schedule schedule)])
