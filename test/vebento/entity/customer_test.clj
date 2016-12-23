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
             :refer [def-scenario mock-journal mock-dispatcher]]
            [vebento.entity.product
             :as product]
            [vebento.entity.order
             :as order]
            [vebento.entity.retailer
             :as retailer]
            [vebento.entity.customer
             :as customer]))


(defn test-bench []
  (-> (co/system-map
        :aggregates
        (entity/aggregates core/aggregate-context)
        :dispatcher
        (mock-dispatcher)
        :journal
        (mock-journal)
        :customer
        (customer/->Component nil nil nil nil)
        :retailer
        (retailer/->Component nil nil nil nil)
        :product
        (product/->Component nil nil nil nil)
        :order
        (order/->Component nil nil nil nil))
      (co/system-using
        {:customer [:aggregates :dispatcher :journal]
         :retailer [:aggregates :dispatcher :journal]
         :order [:aggregates :dispatcher :journal]
         :product [:aggregates :dispatcher :journal]
         :dispatcher [:journal]})
      (co/start)
      (:customer)))



(def-scenario customer-gets-registered
  [::customer/id customer-id]
  :using (test-bench)
  :after [(command
            ::customer/register
            ::customer/id customer-id)]
  :await [(message
            ::customer/registered
            ::customer/id customer-id)])

(def-scenario customer-gets-registered-with-address
  [::customer/id customer-id
   ::customer/address customer-address]
  :using (test-bench)
  :after [(command
            ::customer/register
            ::customer/id customer-id
            ::customer/address customer-address)]
  :await [(message
            ::customer/registered
            ::customer/id customer-id)
          (message
            ::customer/address-changed
            ::customer/id customer-id
            ::customer/address customer-address)])

(def-scenario customer-can-register-only-once
  [::customer/id customer-id]
  :using (test-bench)
  :given [(command
            ::customer/register
            ::customer/id customer-id)]
  :after [(command
            ::customer/register
            ::customer/id customer-id)]
  :await [(failure
            ::entity/already-exists
            ::entity/id-key ::customer/id
            ::entity/id customer-id)])

(def-scenario customer-cart-gets-cleared
  [::customer/id customer-id]
  :using (test-bench)
  :given [(command
            ::customer/register
            ::customer/id customer-id)]
  :after [(command
            ::customer/clear-cart
            ::customer/id customer-id)]
  :await [(message
            ::customer/cart-cleared
            ::customer/id customer-id)])

(def-scenario customer-cart-can-only-be-cleared-if-customer-exists
  [::customer/id customer-id]
  :using (test-bench)
  :after [(command
            ::customer/clear-cart
            ::customer/id customer-id)]
  :await [(failure
            ::entity/not-found
            ::entity/id-key ::customer/id
            ::entity/id customer-id)])

(def-scenario retailer-gets-selected
  [::customer/id customer-id
   ::retailer/id retailer-id
   ::customer/address customer-address
   ::retailer/address retailer-address]
  :using (test-bench)
  :given [(command
            ::retailer/register
            ::retailer/id retailer-id
            ::retailer/address retailer-address)
          (command
            ::retailer/add-area
            ::retailer/id retailer-id
            ::retailer/zipcode (::specs/zipcode customer-address))
          (command
            ::customer/register
            ::customer/id customer-id
            ::customer/address customer-address)]
  :after [(command
            ::customer/select-retailer
            ::customer/id customer-id
            ::retailer/id retailer-id)]
  :await [(message
            ::customer/retailer-selected
            ::customer/id customer-id
            ::retailer/id retailer-id)])

    ;(def-scenario retailer-can-only-be-selected-if-customer-exists
      ;:using (test-bench)
      ;:after [(command
                ;::customer/select-retailer
                ;::customer/id customer-id
                ;::retailer/id retailer-id)]
      ;:await [(failure
                ;::entity/not-found
                ;::entity/id-key ::customer/id
                ;::entity/id customer-id)])

    ;(def-scenario select-retailer-fails-unless-address-was-given
      ;:using (test-bench)
      ;:given [(command
                ;::retailer/register
                ;::retailer/id retailer-id
                ;::retailer/address retailer-address)
              ;(command
                ;::retailer/add-area
                ;::retailer/id retailer-id
                ;::retailer/zipcode (::specs/zipcode customer-address))
              ;(command
                ;::customer/register
                ;::customer/id customer-id)]
      ;:after [(command
                ;::customer/select-retailer
                ;::customer/id customer-id
                ;::retailer/id retailer-id)]
      ;:await [(failure
                ;::customer/has-given-no-address
                ;::customer/id customer-id)])

    ;(def-scenario select-retailer-fails-unless-he-delivers-in-the-customers-area
      ;:using (test-bench)
      ;:given [(command
                ;::retailer/register
                ;::retailer/id retailer-id
                ;::retailer/address retailer-address)
              ;(command
                ;::customer/register
                ;::customer/id customer-id
                ;::customer/address customer-address)]
      ;:after [(command
                ;::customer/select-retailer
                ;::customer/id customer-id
                ;::retailer/id retailer-id)]
      ;:await [(failure
                ;::customer/zipcode-not-in-retailer-areas
                ;::customer/id customer-id
                ;::customer/zipcode (::specs/zipcode customer-address))])

    ;(def-scenario item-gets-added-to-customer-cart
      ;:using (test-bench)
      ;:given [(command
                ;::product/create
                ;::product/id product-id
                ;::product/name product-name)
              ;(command
                ;::retailer/register
                ;::retailer/id retailer-id
                ;::retailer/address retailer-address)
              ;(command
                ;::retailer/add-product
                ;::retailer/id retailer-id
                ;::product/id product-id)
              ;(command
                ;::retailer/add-area
                ;::retailer/id retailer-id
                ;::retailer/zipcode (::specs/zipcode customer-address))
              ;(command
                ;::customer/register
                ;::customer/id customer-id
                ;::customer/address customer-address
                ;::retailer/id retailer-id)]
      ;:after [(command
                ;::customer/add-item-to-cart
                ;::customer/id customer-id
                ;::product/id product-id
                ;::product/amount amount)]
      ;:await [(message
                ;::customer/item-added-to-cart
                ;::customer/id customer-id
                ;::product/id product-id
                ;::product/amount amount)])

    ;(def-scenario add-item-fails-unless-retailer-sells-the-selected-product
      ;:using (test-bench)
      ;:given [(command
                ;::product/create
                ;::product/id product-id
                ;::product/name product-name)
              ;(command
                ;::retailer/register
                ;::retailer/id retailer-id
                ;::retailer/address retailer-address)
              ;(command
                ;::retailer/add-area
                ;::retailer/id retailer-id
                ;::retailer/zipcode (::specs/zipcode customer-address))
              ;(command
                ;::customer/register
                ;::customer/id customer-id
                ;::customer/address customer-address
                ;::retailer/id retailer-id)]
      ;:after [(command
                ;::customer/add-item-to-cart
                ;::customer/id customer-id
                ;::product/id product-id
                ;::product/amount amount)]
      ;:await [(failure
                ;::customer/product-not-in-retailer-assortment
                ;::customer/id customer-id
                ;::product/id product-id)])

    ;(def-scenario customer-order-gets-placed
      ;:using (test-bench)
      ;:given [(command
                ;::product/create
                ;::product/id product-id
                ;::product/name product-name)
              ;(command
                ;::retailer/register
                ;::retailer/id retailer-id
                ;::retailer/address retailer-address)
              ;(command
                ;::retailer/add-product
                ;::retailer/id retailer-id
                ;::product/id product-id)
              ;(command
                ;::retailer/add-area
                ;::retailer/id retailer-id
                ;::retailer/zipcode (::specs/zipcode customer-address))
              ;(command
                ;::retailer/add-schedule
                ;::retailer/id retailer-id
                ;::retailer/schedule schedule)
              ;(command
                ;::retailer/add-payment-method
                ;::retailer/id retailer-id
                ;::retailer/payment-method payment-method)
              ;(command
                ;::customer/register
                ;::customer/id customer-id
                ;::customer/address customer-address
                ;::retailer/id retailer-id)
              ;(command
                ;::customer/select-schedule
                ;::customer/id customer-id
                ;::customer/schedule schedule)
              ;(command
                ;::customer/select-payment-method
                ;::customer/id customer-id
                ;::customer/payment-method payment-method)
              ;(command
                ;::customer/add-item-to-cart
                ;::customer/id customer-id
                ;::product/id product-id
                ;::product/amount amount)]
      ;:after [(command
                ;::customer/place-order
                ;::customer/id customer-id
                ;::order/id order-id)]
      ;:await [(message
                ;::order/placed
                ;::order/id order-id
                ;::customer/id customer-id
                ;::retailer/id retailer-id
                ;::order/items cart
                ;::order/address customer-address
                ;::order/schedule schedule
                ;::order/payment-method payment-method)
              ;(message
                ;::customer/cart-cleared
                ;::customer/id customer-id)])

    ;(def-scenario place-customer-order-fails-when-cart-is-empty
      ;:using (test-bench)
      ;:given [(command
                ;::product/create
                ;::product/id product-id
                ;::product/name product-name)
              ;(command
                ;::retailer/register
                ;::retailer/id retailer-id
                ;::retailer/address retailer-address)
              ;(command
                ;::retailer/add-product
                ;::retailer/id retailer-id
                ;::product/id product-id)
              ;(command
                ;::retailer/add-area
                ;::retailer/id retailer-id
                ;::retailer/zipcode (::specs/zipcode customer-address))
              ;(command
                ;::retailer/add-schedule
                ;::retailer/id retailer-id
                ;::retailer/schedule schedule)
              ;(command
                ;::retailer/add-payment-method
                ;::retailer/id retailer-id
                ;::retailer/payment-method payment-method)
              ;(command
                ;::customer/register
                ;::customer/id customer-id
                ;::customer/address customer-address
                ;::retailer/id retailer-id)
              ;(command
                ;::customer/select-schedule
                ;::customer/id customer-id
                ;::customer/schedule schedule)
              ;(command
                ;::customer/select-payment-method
                ;::customer/id customer-id
                ;::customer/payment-method payment-method)]
      ;:after [(command
                ;::customer/place-order
                ;::customer/id customer-id
                ;::order/id order-id)]
      ;:await [(failure
                ;::customer/cart-is-empty
                ;::customer/id customer-id)])

    ;(def-scenario place-customer-order-fails-unless-address-was-provided
      ;:using (test-bench)
      ;:given [(message
                ;::retailer/registered
                ;::retailer/id retailer-id
                ;::retailer/address retailer-address)
              ;(message
                ;::retailer/area-added
                ;::retailer/id retailer-id
                ;::retailer/zipcode (::specs/zipcode customer-address))
              ;(message
                ;::retailer/schedule-added
                ;::retailer/id retailer-id
                ;::retailer/schedule schedule)
              ;(message
                ;::retailer/payment-method-added
                ;::retailer/id retailer-id
                ;::retailer/payment-method payment-method)
              ;(message
                ;::customer/registered
                ;::customer/id customer-id)
              ;(message
                ;::customer/retailer-selected
                ;::customer/id customer-id
                ;::retailer/id retailer-id)
              ;(message
                ;::customer/schedule-selected
                ;::customer/id customer-id
                ;::customer/schedule schedule)
              ;(message
                ;::customer/payment-method-selected
                ;::customer/id customer-id
                ;::customer/payment-method payment-method)
              ;(message
                ;::customer/item-added-to-cart
                ;::customer/id customer-id
                ;::product/id product-id
                ;::product/amount amount)]
      ;:after [(command
                ;::customer/place-order
                ;::customer/id customer-id
                ;::order/id order-id)]
      ;:await [(failure
                ;::customer/has-given-no-address
                ;::customer/id customer-id)])

    ;(def-scenario place-customer-order-fails-unless-retailer-was-selected
      ;:using (test-bench)
      ;:given [(message
                ;::retailer/registered
                ;::retailer/id retailer-id
                ;::retailer/address retailer-address)
              ;(message
                ;::retailer/area-added
                ;::retailer/id retailer-id
                ;::retailer/zipcode (::specs/zipcode customer-address))
              ;(message
                ;::retailer/schedule-added
                ;::retailer/id retailer-id
                ;::retailer/schedule schedule)
              ;(message
                ;::retailer/payment-method-added
                ;::retailer/id retailer-id
                ;::retailer/payment-method payment-method)
              ;(message
                ;::customer/registered
                ;::customer/id customer-id)
              ;(message
                ;::customer/address-changed
                ;::customer/id customer-id
                ;::customer/address customer-address)
              ;(message
                ;::customer/schedule-selected
                ;::customer/id customer-id
                ;::customer/schedule schedule)
              ;(message
                ;::customer/payment-method-selected
                ;::customer/id customer-id
                ;::customer/payment-method payment-method)
              ;(message
                ;::customer/item-added-to-cart
                ;::customer/id customer-id
                ;::product/id product-id
                ;::product/amount amount)]
      ;:after [(command
                ;::customer/place-order
                ;::customer/id customer-id
                ;::order/id order-id)]
      ;:await [(failure
                ;::customer/has-selected-no-retailer
                ;::customer/id customer-id)])

    ;(def-scenario place-customer-order-fails-unless-schedule-was-selected
      ;:using (test-bench)
      ;:given [(message
                ;::retailer/registered
                ;::retailer/id retailer-id
                ;::retailer/address retailer-address)
              ;(message
                ;::retailer/area-added
                ;::retailer/id retailer-id
                ;::retailer/zipcode (::specs/zipcode customer-address))
              ;(message
                ;::retailer/schedule-added
                ;::retailer/id retailer-id
                ;::retailer/schedule schedule)
              ;(message
                ;::retailer/payment-method-added
                ;::retailer/id retailer-id
                ;::retailer/payment-method payment-method)
              ;(message
                ;::customer/registered
                ;::customer/id customer-id)
              ;(message
                ;::customer/address-changed
                ;::customer/id customer-id
                ;::customer/address customer-address)
              ;(message
                ;::customer/retailer-selected
                ;::customer/id customer-id
                ;::retailer/id retailer-id)
              ;(message
                ;::customer/payment-method-selected
                ;::customer/id customer-id
                ;::customer/payment-method payment-method)
              ;(message
                ;::customer/item-added-to-cart
                ;::customer/id customer-id
                ;::product/id product-id
                ;::product/amount amount)]
      ;:after [(command
                ;::customer/place-order
                ;::customer/id customer-id
                ;::order/id order-id)]
      ;:await [(failure
                ;::customer/has-selected-no-schedule
                ;::customer/id customer-id)])))
