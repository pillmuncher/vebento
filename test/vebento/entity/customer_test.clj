(ns vebento.entity.customer-test
  (:require [clojure.future
             :refer :all]
            [clojure.test
             :refer :all]
            [clojure.spec
             :as s]
            [com.stuartsierra.component
             :as co]
            [util
             :refer [uuid]]
            [juncture.event
             :as event]
            [juncture.entity
             :as entity]
            [juncture.event
             :as event
             :refer [command message failure]]
            [vebento.core
             :as core]
            [vebento.specs
             :as specs]
            [vebento.testing
             :as testing
             :refer [def-scenario mock-journal mock-dispatcher
                     mock-entity-store]]
            [vebento.entity.product
             :as product]
            [vebento.entity.order
             :as order]
            [vebento.entity.merchant
             :as merchant]
            [vebento.entity.customer
             :as customer]))


(defn test-bench []
  (-> (co/system-map
        :aggregates
        (entity/aggregates core/aggregate-context)
        :entity-store
        (mock-entity-store)
        :dispatcher
        (mock-dispatcher)
        :journal
        (mock-journal)
        :customer
        (customer/->Component nil nil nil nil nil)
        :merchant
        (merchant/->Component nil nil nil nil nil)
        :product
        (product/->Component nil nil nil nil nil)
        :order
        (order/->Component nil nil nil nil nil))
      (co/system-using
        {:customer [:aggregates :dispatcher :journal :entity-store]
         :merchant [:aggregates :dispatcher :journal :entity-store]
         :order [:aggregates :dispatcher :journal :entity-store]
         :product [:aggregates :dispatcher :journal :entity-store]
         :dispatcher [:journal]})
      (co/start)
      (:customer)))


(def-scenario customer-gets-registered
  [customer-id ::customer/id]
  :using (test-bench)
  :after [(command
            ::customer/register
            ::customer/id customer-id)]
  :raise [(message
            ::customer/registered
            ::customer/id customer-id)])


(def-scenario customer-gets-registered-with-address
  [customer-id ::customer/id
   customer-address ::customer/address]
  :using (test-bench)
  :after [(command
            ::customer/register
            ::customer/id customer-id
            ::customer/address customer-address)]
  :raise [(message
            ::customer/registered
            ::customer/id customer-id)
          (message
            ::customer/address-changed
            ::customer/id customer-id
            ::customer/address customer-address)])


(def-scenario customer-can-register-only-once
  [customer-id ::customer/id]
  :using (test-bench)
  :given [(command
            ::customer/register
            ::customer/id customer-id)]
  :after [(command
            ::customer/register
            ::customer/id customer-id)]
  :raise [(failure
            ::entity/already-exists
            ::entity/id-key ::customer/id
            ::entity/id customer-id)])


(def-scenario customer-clears-cart
  [customer-id ::customer/id]
  :using (test-bench)
  :given [(command
            ::customer/register
            ::customer/id customer-id)]
  :after [(command
            ::customer/clear-cart
            ::customer/id customer-id)]
  :raise [(message
            ::customer/cart-cleared
            ::customer/id customer-id)])


(def-scenario only-an-existing-customer-can-clear-cart
  [customer-id ::customer/id]
  :using (test-bench)
  :after [(command
            ::customer/clear-cart
            ::customer/id customer-id)]
  :raise [(failure
            ::entity/not-found
            ::entity/id-key ::customer/id
            ::entity/id customer-id)])


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
  :raise [(message
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
  :raise [(failure
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
  :raise [(failure
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
  :raise [(failure
            ::customer/zipcode-not-in-merchant-areas
            ::customer/id customer-id
            ::customer/zipcode (::specs/zipcode customer-address))])


(def-scenario customer-adds-item-to-cart
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
            ::merchant/id merchant-id)]
  :after [(command
            ::customer/add-item-to-cart
            ::customer/id customer-id
            ::product/id product-id
            ::product/amount amount)]
  :raise [(message
            ::customer/item-added-to-cart
            ::customer/id customer-id
            ::product/id product-id
            ::product/amount amount)])


(def-scenario customer-can-only-add-product-to-cart-if-merchant-sells-it
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
            ::merchant/add-area
            ::merchant/id merchant-id
            ::merchant/zipcode (::specs/zipcode customer-address))
          (command
            ::customer/register
            ::customer/id customer-id
            ::customer/address customer-address
            ::merchant/id merchant-id)]
  :after [(command
            ::customer/add-item-to-cart
            ::customer/id customer-id
            ::product/id product-id
            ::product/amount amount)]
  :raise [(failure
            ::customer/product-not-in-merchant-assortment
            ::customer/id customer-id
            ::product/id product-id)])


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
  :after [(command
            ::customer/place-order
            ::customer/id customer-id
            ::order/id order-id)]
  :raise [(message
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
            ::merchant/id merchant-id)
          (command
            ::customer/add-schedule
            ::customer/id customer-id
            ::customer/schedule schedule)
          (command
            ::customer/select-payment-method
            ::customer/id customer-id
            ::customer/payment-method payment-method)]
  :after [(command
            ::customer/place-order
            ::customer/id customer-id
            ::order/id order-id)]
  :raise [(failure
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
            ::merchant/id merchant-id)
          (command
            ::customer/select-payment-method
            ::customer/id customer-id
            ::customer/payment-method payment-method)
          (command
            ::customer/add-item-to-cart
            ::customer/id customer-id
            ::product/id product-id
            ::product/amount amount)]
  :after [(command
            ::customer/place-order
            ::customer/id customer-id
            ::order/id order-id)]
  :raise [(failure
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
  :after [(command
            ::customer/place-order
            ::customer/id customer-id
            ::order/id order-id)]
  :raise [(failure
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
