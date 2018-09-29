(ns vebento.customer.places-order-test
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
(ns-alias 'order 'vebento.order)
(ns-alias 'merchant 'vebento.merchant)
(ns-alias 'customer 'vebento.customer)


(def-scenario customer-places-order
  [customer-id ::customer/id
   customer-address ::customer/address
   merchant-id ::merchant/id
   merchant-address ::merchant/address
   product-id ::product/id
   product-name ::product/name
   amount ::product/amount
   order-id ::order/id
   payment-method ::order/payment-method
   schedule ::order/schedule]
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
            ::merchant/add-schedule
            ::merchant/id merchant-id
            ::merchant/schedule schedule)
          (command
            ::merchant/add-payment-method
            ::merchant/id merchant-id
            ::merchant/payment-method payment-method)
          (command
            ::customer/register
            ::customer/id customer-id
            ::customer/address customer-address
            ::merchant/id merchant-id)
          (command
            ::customer/add-schedule
            ::customer/id customer-id
            ::customer/schedule schedule)
          (command
            ::customer/select-payment-method
            ::customer/id customer-id
            ::customer/payment-method payment-method)
          (command
            ::customer/add-item-to-cart
            ::customer/id customer-id
            ::product/id product-id
            ::product/amount amount)]
  :when  [(command
            ::customer/place-order
            ::customer/id customer-id
            ::order/id order-id)]
  :then  [(message
            ::order/placed
            ::order/id order-id
            ::customer/id customer-id
            ::merchant/id merchant-id
            ::order/items {product-id amount}
            ::order/address customer-address
            ::order/schedule schedule
            ::order/payment-method payment-method)
          (message
            ::customer/cart-cleared
            ::customer/id customer-id)])


(def-scenario customer-cannot-place-order-when-cart-is-empty
  [customer-id ::customer/id
   customer-address ::customer/address
   merchant-id ::merchant/id
   merchant-address ::merchant/address
   product-id ::product/id
   product-name ::product/name
   amount ::product/amount
   order-id ::order/id
   payment-method ::order/payment-method
   schedule ::order/schedule]
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
            ::merchant/add-schedule
            ::merchant/id merchant-id
            ::merchant/schedule schedule)
          (command
            ::merchant/add-payment-method
            ::merchant/id merchant-id
            ::merchant/payment-method payment-method)
          (command
            ::customer/register
            ::customer/id customer-id
            ::customer/address customer-address
            ::merchant/id merchant-id
            ::customer/payment-method payment-method)
          (command
            ::customer/add-schedule
            ::customer/id customer-id
            ::customer/schedule schedule)]
  :when  [(command
            ::customer/place-order
            ::customer/id customer-id
            ::order/id order-id)]
  :then  [(failure
            ::customer/cart-is-empty
            ::customer/id customer-id)])


(def-scenario customer-cannot-place-order-unless-schedule-was-selected
  [customer-id ::customer/id
   customer-address ::customer/address
   merchant-id ::merchant/id
   merchant-address ::merchant/address
   product-id ::product/id
   product-name ::product/name
   amount ::product/amount
   order-id ::order/id
   payment-method ::order/payment-method
   schedule ::order/schedule]
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
            ::merchant/add-schedule
            ::merchant/id merchant-id
            ::merchant/schedule schedule)
          (command
            ::merchant/add-payment-method
            ::merchant/id merchant-id
            ::merchant/payment-method payment-method)
          (command
            ::customer/register
            ::customer/id customer-id
            ::customer/address customer-address
            ::merchant/id merchant-id
            ::customer/payment-method payment-method)
          (command
            ::customer/add-item-to-cart
            ::customer/id customer-id
            ::product/id product-id
            ::product/amount amount)]
  :when  [(command
            ::customer/place-order
            ::customer/id customer-id
            ::order/id order-id)]
  :then  [(failure
            ::customer/has-selected-no-schedule
            ::customer/id customer-id)])


(def-scenario customer-cannot-place-order
  [customer-id ::customer/id
   customer-address ::customer/address
   merchant-id ::merchant/id
   merchant-address ::merchant/address
   product-id ::product/id
   product-name ::product/name
   amount ::product/amount
   order-id ::order/id
   payment-method ::order/payment-method
   schedule ::order/schedule]
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
            ::merchant/add-schedule
            ::merchant/id merchant-id
            ::merchant/schedule schedule)
          (command
            ::merchant/add-payment-method
            ::merchant/id merchant-id
            ::merchant/payment-method payment-method)
          (command
            ::customer/register
            ::customer/id customer-id)]
  :when  [(command
            ::customer/place-order
            ::customer/id customer-id
            ::order/id order-id)]
  :then  [(failure
            ::customer/cart-is-empty
            ::customer/id customer-id)
          (failure
            ::customer/has-given-no-address
            ::customer/id customer-id)
          (failure
            ::customer/has-selected-no-payment-method
            ::customer/id customer-id)
          (failure
            ::customer/has-selected-no-merchant
            ::customer/id customer-id)
          (failure
            ::customer/has-selected-no-schedule
            ::customer/id customer-id)])
