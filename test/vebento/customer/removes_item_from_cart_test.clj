(ns vebento.customer.removes-item-from-cart-test
  (:require [clojure.test
             :refer :all]
            [util
             :refer [ns-alias]]
            [juncture.event
             :refer [command message failure]]
            [vebento.testing
             :refer [def-scenario]]
            [vebento.customer-test
             :refer [test-bench]]))


(ns-alias 'specs 'vebento.specs)
(ns-alias 'product 'vebento.product)
(ns-alias 'merchant 'vebento.merchant)
(ns-alias 'customer 'vebento.customer)


(def-scenario customer-removes-item-from-cart
  [customer-id ::customer/id
   merchant-id ::merchant/id
   customer-address ::customer/address
   merchant-address ::merchant/address
   product-id ::product/id
   product-name ::product/name
   amount ::product/amount]
  :using (test-bench)
  :given [(command
            ::product/create
            ::product/id product-id
            ::product/name product-name)
          (command
            ::merchant/register
            ::merchant/id merchant-id
            ::merchant/address merchant-address)
          (command
            ::merchant/add-product
            ::merchant/id merchant-id
            ::product/id product-id)
          (command
            ::merchant/add-area
            ::merchant/id merchant-id
            ::merchant/zipcode (::specs/zipcode customer-address))
          (command
            ::customer/register
            ::customer/id customer-id
            ::customer/address customer-address
            ::merchant/id merchant-id)
          (command
            ::customer/add-item-to-cart
            ::customer/id customer-id
            ::product/id product-id
            ::product/amount amount)]
  :after [(command
            ::customer/remove-item-from-cart
            ::customer/id customer-id
            ::product/id product-id)]
  :yield [(message
            ::customer/item-removed-from-cart
            ::customer/id customer-id
            ::product/id product-id)])


(def-scenario customer-removes-item-from-cart
  [customer-id ::customer/id
   merchant-id ::merchant/id
   customer-address ::customer/address
   merchant-address ::merchant/address
   product-id ::product/id
   product-name ::product/name]
  :using (test-bench)
  :given [(command
            ::product/create
            ::product/id product-id
            ::product/name product-name)
          (command
            ::merchant/register
            ::merchant/id merchant-id
            ::merchant/address merchant-address)
          (command
            ::merchant/add-product
            ::merchant/id merchant-id
            ::product/id product-id)
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
            ::customer/remove-item-from-cart
            ::customer/id customer-id
            ::product/id product-id)]
  :yield [(failure
            ::customer/product-not-in-cart
            ::customer/id customer-id
            ::product/id product-id)])
