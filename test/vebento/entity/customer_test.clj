(ns vebento.entity.customer-test
  (:require [clojure.future
             :refer :all]
            [clojure.test
             :refer :all]
            [com.stuartsierra.component
             :as co]
            [util
             :refer [uuid]]
            [juncture.event
             :as event]
            [juncture.entity
             :as entity]
            [componad
             :as componad
             :refer [within system]]
            [vebento.core
             :as core
             :refer [return-command return-message return-failure]]
            [vebento.specs
             :as specs]
            [vebento.testing
             :as testing
             :refer [def-scenario given after expect mock-journal
                     mock-dispatcher]]
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
      (co/start)))


;(clojure.test/run-tests)


(let [customer-id (uuid)
      retailer-id (uuid)
      order-id (uuid)
      product-id (uuid)
      product-name "schnitzel"
      amount 3
      cart {product-id amount}
      payment-method "PayPal"
      schedule #{{::retailer/schedule-time-of-day "vormittags"
                  ::retailer/schedule-is-recurrent false}}
      retailer-address {::specs/first-name "Bruce"
                        ::specs/last-name "Wayne"
                        ::specs/street "Dark Castle 1"
                        ::specs/zipcode "54321"
                        ::specs/city "Gotham City"
                        ::specs/email "bruce.wayne@crumb.com"}
      customer-address {::specs/first-name "Donald"
                        ::specs/last-name "Duck"
                        ::specs/street "Erpelweg 13"
                        ::specs/zipcode "12345"
                        ::specs/city "Entenhausen"
                        ::specs/email "donald.duck@barks.com"}]

  (def-scenario customer-gets-registered
    (within (system (test-bench))
      (after
        (return-command
          ::customer/register
          ::customer/id customer-id))
      (expect
        (return-message
          ::customer/registered
          ::customer/id customer-id))))

  (def-scenario customer-gets-registered-with-address
    (within (system (test-bench))
      (after
        (return-command
          ::customer/register
          ::customer/id customer-id
          ::customer/address customer-address))
      (expect
        (return-message
          ::customer/registered
          ::customer/id customer-id)
        (return-message
          ::customer/address-changed
          ::customer/id customer-id
          ::customer/address customer-address))))

  (def-scenario customer-can-register-only-once
    (within (system (test-bench))
      (given
        (return-command
          ::customer/register
          ::customer/id customer-id))
      (after
        (return-command
          ::customer/register
          ::customer/id customer-id))
      (expect
        (return-failure
          ::entity/already-exists
          ::entity/id-key ::customer/id
          ::entity/id customer-id))))

  (def-scenario customer-cart-gets-cleared
    (within (system (test-bench))
      (given
        (return-command
          ::customer/register
          ::customer/id customer-id))
      (after
        (return-command
          ::customer/clear-cart
          ::customer/id customer-id))
      (expect
        (return-message
          ::customer/cart-cleared
          ::customer/id customer-id))))

  (def-scenario customer-cart-can-only-be-cleared-if-customer-exists
    (within (system (test-bench))
      (after
        (return-command
          ::customer/clear-cart
          ::customer/id customer-id))
      (expect
        (return-failure
          ::entity/not-found
          ::entity/id-key ::customer/id
          ::entity/id customer-id))))

  (def-scenario retailer-gets-selected
    (within (system (test-bench))
      (given
        (return-command
          ::retailer/register
          ::retailer/id retailer-id
          ::retailer/address retailer-address)
        (return-command
          ::retailer/add-area
          ::retailer/id retailer-id
          ::retailer/zipcode (::customer/zipcode customer-address))
        (return-command
          ::customer/register
          ::customer/id customer-id
          ::customer/address customer-address))
      (after
        (return-command
          ::customer/select-retailer
          ::customer/id customer-id
          ::retailer/id retailer-id))
      (expect
        (return-message
          ::customer/retailer-selected
          ::customer/id customer-id
          ::retailer/id retailer-id))))

  (def-scenario retailer-can-only-be-selected-if-customer-exists
    (within (system (test-bench))
      (after
        (return-command
          ::customer/select-retailer
          ::customer/id customer-id
          ::retailer/id retailer-id))
      (expect
        (return-failure
          ::entity/not-found
          ::entity/id-key ::customer/id
          ::entity/id customer-id))))

  (def-scenario select-retailer-fails-unless-address-was-given
    (within (system (test-bench))
      (given
        (return-command
          ::retailer/register
          ::retailer/id retailer-id
          ::retailer/address retailer-address)
        (return-command
          ::retailer/add-area
          ::retailer/id retailer-id
          ::retailer/zipcode (:zipcode customer-address))
        (return-command
          ::customer/register
          ::customer/id customer-id))
      (after
        (return-command
          ::customer/select-retailer
          ::customer/id customer-id
          ::retailer/id retailer-id))
      (expect
        (return-failure
          ::customer/has-given-no-address
          ::customer/id customer-id))))

  (def-scenario select-retailer-fails-unless-he-delivers-in-the-customers-area
    (within (system (test-bench))
      (given
        (return-command
          ::retailer/register
          ::retailer/id retailer-id
          ::retailer/address retailer-address)
        (return-command
          ::customer/register
          ::customer/id customer-id
          ::customer/address customer-address))
      (after
        (return-command
          ::customer/select-retailer
          ::customer/id customer-id
          ::retailer/id retailer-id))
      (expect
        (return-failure
          ::customer/zipcode-not-in-retailer-areas
          ::customer/id customer-id
          ::customer/zipcode (::customer/zipcode customer-address)))))

  (def-scenario item-gets-added-to-customer-cart
    (within (system (test-bench))
      (given
        (return-command
          ::product/create
          ::product/id product-id
          ::product/name product-name)
        (return-command
          ::retailer/register
          ::retailer/id retailer-id
          ::retailer/address retailer-address)
        (return-command
          ::retailer/add-product
          ::retailer/id retailer-id
          ::product/id product-id)
        (return-command
          ::retailer/add-area
          ::retailer/id retailer-id
          ::retailer/zipcode (:zipcode customer-address))
        (return-command
          ::customer/register
          ::customer/id customer-id
          ::customer/address customer-address
          ::retailer/id retailer-id))
      (after
        (return-command
          ::customer/add-item-to-cart
          ::customer/id customer-id
          ::product/id product-id
          ::product/amount amount))
      (expect
        (return-message
          ::customer/item-added-to-cart
          ::customer/id customer-id
          ::product/id product-id
          ::product/amount amount))))

  (def-scenario add-item-fails-unless-retailer-sells-the-selected-product
    (within (system (test-bench))
      (given
        (return-command
          ::product/create
          ::product/id product-id
          ::product/name product-name)
        (return-command
          ::retailer/register
          ::retailer/id retailer-id
          ::retailer/address retailer-address)
        (return-command
          ::retailer/add-area
          ::retailer/id retailer-id
          ::retailer/zipcode (:zipcode customer-address))
        (return-command
          ::customer/register
          ::customer/id customer-id
          ::customer/address customer-address
          ::retailer/id retailer-id))
      (after
        (return-command
          ::customer/add-item-to-cart
          ::customer/id customer-id
          ::product/id product-id
          ::product/amount amount))
      (expect
        (return-failure
          ::customer/product-not-in-retailer-assortment
          ::customer/id customer-id
          ::product/id product-id))))

  (def-scenario customer-order-gets-placed
    (within (system (test-bench))
      (given
        (return-message
          ::retailer/registered
          ::retailer/id retailer-id
          ::retailer/address retailer-address)
        (return-message
          ::retailer/area-added
          ::retailer/id retailer-id
          ::retailer/zipcode (::specs/zipcode customer-address))
        (return-message
          ::retailer/schedule-added
          ::retailer/id retailer-id
          ::retailer/schedule schedule)
        (return-message ::retailer/payment-method-added
                        ::retailer/id retailer-id
                        ::retailer/payment-method payment-method)
        (return-message
          ::customer/registered
          ::customer/id customer-id)
        (return-message
          ::customer/address-changed
          ::customer/id customer-id
          ::customer/address customer-address)
        (return-message
          ::customer/retailer-selected
          ::customer/id customer-id
          ::retailer/id retailer-id)
        (return-message
          ::customer/schedule-selected
          ::customer/id customer-id
          ::customer/schedule schedule)
        (return-message
          ::customer/payment-method-selected
          ::customer/id customer-id
          ::customer/payment-method payment-method)
        (return-message
          ::customer/item-added-to-cart
          ::customer/id customer-id
          ::product/id product-id
          ::product/amount amount))
      (after
        (return-command
          ::customer/place-order
          ::customer/id customer-id
          ::order/id order-id))
      (expect
        (return-message
          ::order/placed
          ::order/id order-id
          ::customer/id customer-id
          ::retailer/id retailer-id
          ::order/items cart
          ::order/address customer-address
          ::order/schedule schedule
          ::order/payment-method payment-method)
        (return-message
          ::customer/cart-cleared
          ::customer/id customer-id))))

  (def-scenario place-customer-order-fails-when-cart-is-empty
    (within (system (test-bench))
      (given
        (return-message
          ::retailer/registered
          ::retailer/id retailer-id
          ::retailer/address retailer-address)
        (return-message
          ::retailer/area-added
          ::retailer/id retailer-id
          ::retailer/zipcode (::specs/zipcode customer-address))
        (return-message
          ::retailer/schedule-added
          ::retailer/id retailer-id
          ::retailer/schedule schedule)
        (return-message ::retailer/payment-method-added
                        ::retailer/id retailer-id
                        ::retailer/payment-method payment-method)
        (return-message
          ::customer/registered
          ::customer/id customer-id)
        (return-message
          ::customer/address-changed
          ::customer/id customer-id
          ::customer/address customer-address)
        (return-message
          ::customer/retailer-selected
          ::customer/id customer-id
          ::retailer/id retailer-id)
        (return-message
          ::customer/schedule-selected
          ::customer/id customer-id
          ::customer/schedule schedule)
        (return-message
          ::customer/payment-method-selected
          ::customer/id customer-id
          ::customer/payment-method payment-method))
      (after
        (return-command
          ::customer/place-order
          ::customer/id customer-id
          ::order/id order-id))
      (expect
        (return-failure
          ::customer/cart-is-empty
          ::customer/id customer-id))))

  (def-scenario place-customer-order-fails-unless-address-was-provided
    (within (system (test-bench))
      (given
        (return-message
          ::retailer/registered
          ::retailer/id retailer-id
          ::retailer/address retailer-address)
        (return-message
          ::retailer/area-added
          ::retailer/id retailer-id
          ::retailer/zipcode (::specs/zipcode customer-address))
        (return-message
          ::retailer/schedule-added
          ::retailer/id retailer-id
          ::retailer/schedule schedule)
        (return-message ::retailer/payment-method-added
                        ::retailer/id retailer-id
                        ::retailer/payment-method payment-method)
        (return-message
          ::customer/registered
          ::customer/id customer-id)
        (return-message
          ::customer/retailer-selected
          ::customer/id customer-id
          ::retailer/id retailer-id)
        (return-message
          ::customer/schedule-selected
          ::customer/id customer-id
          ::customer/schedule schedule)
        (return-message
          ::customer/payment-method-selected
          ::customer/id customer-id
          ::customer/payment-method payment-method)
        (return-message
          ::customer/item-added-to-cart
          ::customer/id customer-id
          ::product/id product-id
          ::product/amount amount))
      (after
        (return-command
          ::customer/place-order
          ::customer/id customer-id
          ::order/id order-id))
      (expect
        (return-failure
          ::customer/has-given-no-address
          ::customer/id customer-id))))

  (def-scenario place-customer-order-fails-unless-retailer-was-selected
    (within (system (test-bench))
      (given
        (return-message
          ::retailer/registered
          ::retailer/id retailer-id
          ::retailer/address retailer-address)
        (return-message
          ::retailer/area-added
          ::retailer/id retailer-id
          ::retailer/zipcode (::specs/zipcode customer-address))
        (return-message
          ::retailer/schedule-added
          ::retailer/id retailer-id
          ::retailer/schedule schedule)
        (return-message ::retailer/payment-method-added
                        ::retailer/id retailer-id
                        ::retailer/payment-method payment-method)
        (return-message
          ::customer/registered
          ::customer/id customer-id)
        (return-message
          ::customer/address-changed
          ::customer/id customer-id
          ::customer/address customer-address)
        (return-message
          ::customer/schedule-selected
          ::customer/id customer-id
          ::customer/schedule schedule)
        (return-message
          ::customer/payment-method-selected
          ::customer/id customer-id
          ::customer/payment-method payment-method)
        (return-message
          ::customer/item-added-to-cart
          ::customer/id customer-id
          ::product/id product-id
          ::product/amount amount))
      (after
        (return-command
          ::customer/place-order
          ::customer/id customer-id
          ::order/id order-id))
      (expect
        (return-failure
          ::customer/has-selected-no-retailer
          ::customer/id customer-id))))

  (def-scenario place-customer-order-fails-unless-schedule-was-selected
    (within (system (test-bench))
      (given
        (return-message
          ::retailer/registered
          ::retailer/id retailer-id
          ::retailer/address retailer-address)
        (return-message
          ::retailer/area-added
          ::retailer/id retailer-id
          ::retailer/zipcode (::specs/zipcode customer-address))
        (return-message
          ::retailer/schedule-added
          ::retailer/id retailer-id
          ::retailer/schedule schedule)
        (return-message ::retailer/payment-method-added
                        ::retailer/id retailer-id
                        ::retailer/payment-method payment-method)
        (return-message
          ::customer/registered
          ::customer/id customer-id)
        (return-message
          ::customer/address-changed
          ::customer/id customer-id
          ::customer/address customer-address)
        (return-message
          ::customer/retailer-selected
          ::customer/id customer-id
          ::retailer/id retailer-id)
        (return-message
          ::customer/payment-method-selected
          ::customer/id customer-id
          ::customer/payment-method payment-method)
        (return-message
          ::customer/item-added-to-cart
          ::customer/id customer-id
          ::product/id product-id
          ::product/amount amount))
      (after
        (return-command
          ::customer/place-order
          ::customer/id customer-id
          ::order/id order-id))
      (expect
        (return-failure
          ::customer/has-selected-no-schedule
          ::customer/id customer-id)))))
