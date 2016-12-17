(ns vebento.entity.customer-test
  (:require [clojure.future
             :refer :all]
            [com.stuartsierra.component
             :as co]
            [juncture.componad
             :refer [within system]]
            [juncture.core
             :as ju]
            [juncture.util
             :refer [ns-alias uuid]]
            [juncture.event
             :as event
             :refer [command message failure]]
            [juncture.testing
             :as testing
             :refer [def-scenario
                     given
                     after
                     expect
                     testing-event-log
                     testing-event-dispatcher]]
            [vebento.specs
             :as specs]
            [vebento.entity.retailer
             :as retailer]
            [vebento.entity.customer
             :as customer]))


(ns-alias 'entity 'juncture.entity)
(ns-alias 'customer 'vebento.entity.customer)
(ns-alias 'retailer 'vebento.entity.retailer)
(ns-alias 'product 'vebento.entity.product)
(ns-alias 'order 'vebento.entity.order)


(defn test-bench []
  (-> (co/system-map
        :customer
        (customer/->Component nil nil nil)
        :customer
        (retailer/->Component nil nil nil)
        :dispatcher
        (testing-event-dispatcher)
        :event-log
        (testing-event-log))
      (co/system-using
        {:customer [:event-log :dispatcher]
        :dispatcher [:event-log]})
      (co/start)))




(def-scenario customer-order-gets-placed
  (print))

(clojure.test/run-tests)


(let [customer-id (uuid)
      retailer-id (uuid)
      product-id (uuid)
      order-id (uuid)
      amount 3
      schedule {::retailer/schedule-time-of-day "vormittags"
                ::retailer/schedule-is-recurrent false}
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
        (command
          ::customer/register
          ::customer/id customer-id))
      (expect
        (message
          ::customer/registered
          ::customer/id customer-id))))

  (def-scenario customer-gets-registered-even-without-address
    (within (system (test-bench))
      (after
        (command
          ::customer/register
          ::customer/id customer-id
          ::customer/address customer-address))
      (expect
        (message
          ::customer/registered
          ::customer/id customer-id)
        (message
          ::customer/address-changed
          ::customer/id customer-id
          ::customer/address customer-address))))

  (def-scenario customer-can-register-only-once
    (within (system (test-bench))
      (given
        (message
          ::customer/registered
          ::customer/id customer-id))
      (after
        (command
          ::customer/register
          ::customer/id customer-id))
      (expect
        (failure
          ::entity/already-exists
          ::entity/id-key ::customer/id
          ::entity/id customer-id))))

  (def-scenario customer-cart-gets-cleared
    (within (system (test-bench))
      (given
        (message
          ::customer/registered
          ::customer/id customer-id))
      (after
        (command
          ::customer/clear-cart
          ::customer/id customer-id))
      (expect
        (message
          ::customer/cart-cleared
          ::customer/id customer-id))))

  (def-scenario customer-cart-can-only-be-cleared-if-customer-exists
    (within (system (test-bench))
      (after
        (command
          ::customer/clear-cart
          ::customer/id customer-id))
      (expect
        (failure
          ::entity/not-found
          ::entity/id-key ::customer/id
          ::entity/id customer-id))))

  (def-scenario retailer-gets-selected
    (within (system (test-bench))
      (given
        (message
          ::retailer/registered
          ::retailer/id retailer-id
          ::retailer/address retailer-address)
        (message
          ::retailer/area-added
          ::retailer/id retailer-id
          ::retailer/zipcode (::customer/zipcode customer-address))
        (message
          ::customer/registered
          ::customer/id customer-id
          ::customer/address customer-address))
      (after
        (command
          ::customer/select-retailer
          ::customer/id customer-id
          ::retailer/id retailer-id))
      (expect
        (message
          ::customer/retailer-selected
          ::customer/id customer-id
          ::retailer/id retailer-id))))

  (def-scenario retailer-can-only-be-selected-if-customer-exists
    (within (system (test-bench))
      (after
        (command
          ::customer/select-retailer
          ::customer/id customer-id
          ::retailer/id retailer-id))
      (expect
        (failure
          ::entity/not-found
          ::entity/id-key ::customer/id
          ::entity/id customer-id))))

  (def-scenario select-retailer-fails-unless-address-was-given
    (within (system (test-bench))
      (given
        (message
          ::retailer/registered
          ::retailer/id retailer-id
          ::retailer/address retailer-address)
        (message
          ::retailer/area-added
          ::retailer/id retailer-id
          ::retailer/zipcode (:zipcode customer-address))
        (message
          ::customer/registered
          ::customer/id customer-id))
      (after
        (command
          ::customer/select-retailer
          ::customer/id customer-id
          ::retailer/id retailer-id))
      (expect
        (failure
          ::customer/has-given-no-address
          ::customer/id customer-id))))

  (def-scenario select-retailer-fails-unless-he-delivers-in-the-customers-area
    (within (system (test-bench))
      (given
        (message
          ::retailer/registered
          ::retailer/id retailer-id
          ::retailer/address retailer-address)
        (message
          ::customer/registered
          ::customer/id customer-id
          ::customer/address customer-address))
      (after
        (command
          ::customer/select-retailer
          ::customer/id customer-id
          ::retailer/id retailer-id))
      (expect
        (failure
          ::customer/zipcode-not-in-retailer-areas
          ::customer/id customer-id
          ::customer/zipcode (::customer/zipcode customer-address)))))

  (def-scenario item-gets-added-to-customers-cart
    (within (system (test-bench))
      (given
        (message
          ::retailer/registered
          ::retailer/id retailer-id
          ::retailer/address retailer-address)
        (message
          ::retailer/product-added
          ::retailer/id retailer-id
          ::product/id product-id)
        (message
          ::customer/registered
          ::customer/id customer-id
          ::customer/address customer-address)
        (message
          ::customer/retailer-selected
          ::customer/id customer-id
          ::retailer/id retailer-id))
      (after
        (command
          ::customer/add-item-to-cart
          ::customer/id customer-id
          ::product/id product-id
          ::product/amount amount))
      (expect
        (message
          ::customer/item-added-to-cart
          ::customer/id customer-id
          ::product/id product-id
          ::product/amount amount))))

  (def-scenario add-item-fails-unless-retailer-sells-the-selected-product
    (within (system (test-bench))
      (given
        (message
          ::retailer/registered
          ::retailer/id retailer-id
          ::retailer/address retailer-address)
        (message
          ::customer/registered
          ::customer/id customer-id
          ::customer/address customer-address)
        (message
          ::customer/retailer-selected
          ::customer/id customer-id
          ::retailer/id retailer-id))
      (after
        (command
          ::customer/add-item-to-cart
          ::customer/id customer-id
          ::product/id product-id
          ::product/amount amount))
      (expect
        (failure
          ::customer/product-not-in-retailer-assortment
          ::customer/id customer-id
          ::product/id product-id)))))

  ;(def-scenario customer-order-gets-placed
    ;(within (system (test-bench))
      ;(given
        ;(message
          ;::retailer/registered
          ;::retailer/id retailer-id
          ;::retailer/address retailer-address)
        ;(message
          ;::retailer/schedule-added
          ;::retailer/id retailer-id
          ;::retailer/schedule schedule)
        ;(message
          ;::customer/registered
          ;::customer/id customer-id
          ;::customer/address customer-address
          ;::retailer/id retailer-id)
        ;(message
          ;::customer/schedule-selected
          ;::customer/id customer-id
          ;::customer/schedule schedule)
        ;(message
          ;::customer/item-added-to-cart
          ;::customer/id customer-id
          ;::product/id product-id
          ;::product/amount amount))
      ;(after
        ;(command
          ;::customer/place-order
          ;::customer/id customer-id
          ;::order/id order-id))
      ;(expect
        ;(message
          ;::customer/order-placed
          ;::customer/id customer-id
          ;::retailer/id retailer-id
          ;::order/id order-id
          ;::order/address customer-address
          ;::order/schedule schedule
          ;::order/items {::product/id product-id
                         ;::product/amount amount})
        ;(message
          ;::customer/cart-cleared
          ;::customer/id customer-id)))))

  ;(def-scenario place-customer-order-fails-when-cart-is-empty
    ;(within (system (test-bench))
      ;(given
        ;(message
          ;::retailer/registered
          ;::retailer/id retailer-id
          ;::retailer/address retailer-address)
        ;(message
          ;::retailer/schedule-added
          ;::retailer/id retailer-id
          ;::retailer/schedule schedule)
        ;(message
          ;::customer/registered
          ;::customer/id customer-id
          ;::customer/address customer-address)
        ;(message
          ;::customer/retailer-selected
          ;::customer/id customer-id
          ;::retailer/id retailer-id)
        ;(message
          ;::customer/schedule-selected
          ;::customer/id customer-id
          ;::customer/schedule schedule)
        ;(after
          ;(command
            ;::customer/place-order
            ;::customer/id customer-id
            ;::order/id order-id))
        ;(expect
          ;(failure
            ;::customer/cart-is-empty)))))

  ;(def-scenario place-customer-order-fails-unless-address-was-provided
    ;(within (system (test-bench))
      ;(given
        ;(message
          ;::retailer/registered
          ;::retailer/id retailer-id
          ;::retailer/address retailer-address)
        ;(message
          ;::retailer/schedule-added
          ;::retailer/id retailer-id
          ;::retailer/schedule schedule)
        ;(message
          ;::customer/registered
          ;::customer/id customer-id
          ;::customer/address nil)
        ;(message
          ;::customer/retailer-selected
          ;::customer/id customer-id
          ;::retailer/id retailer-id)
        ;(message
          ;::customer/schedule-selected
          ;::customer/id customer-id
          ;::customer/schedule schedule)
        ;(message
          ;::customer/item-added-to-cart
          ;::customer/id customer-id
          ;::product/id product-id
          ;::product/amount amount))
      ;(after
        ;(command
          ;::customer/place-order
          ;::customer/id customer-id
          ;::order/id order-id))
      ;(expect
        ;(failure
          ;::customer/no-address-was-provided))))

  ;(def-scenario place-customer-order-fails-unless-retailer-was-selected
    ;(within (system (test-bench))
      ;(given
        ;(message
          ;::retailer/registered
          ;::retailer/id retailer-id
          ;::retailer/address retailer-address)
        ;(message
          ;::retailer/schedule-added
          ;::retailer/id retailer-id
          ;::retailer/schedule schedule)
        ;(message
          ;::customer/registered
          ;::customer/id customer-id
          ;::customer/address customer-address)
        ;(message
          ;::customer/schedule-selected
          ;::customer/id customer-id
          ;::customer/schedule schedule)
        ;(message
          ;::customer/item-added-to-cart
          ;::customer/id customer-id
          ;::product/id product-id
          ;::product/amount amount))
      ;(after
        ;(command
          ;::customer/place-order
          ;::customer/id customer-id
          ;::order/id order-id))
      ;(expect
        ;(failure
          ;::customer/no-retailer-was-selected))))

  ;(def-scenario place-customer-order-fails-unless-schedule-was-selected
    ;(within (system (test-bench))
      ;(given
        ;(message
          ;::retailer/registered
          ;::retailer/id retailer-id
          ;::retailer/address retailer-address)
        ;(message
          ;::retailer/schedule-added
          ;::retailer/id retailer-id
          ;::retailer/schedule schedule)
        ;(message
          ;::customer/registered
          ;::customer/id customer-id
          ;::customer/address customer-address)
        ;(message
          ;::customer/retailer-selected
          ;::customer/id customer-id
          ;::retailer/id retailer-id)
        ;(message
          ;::customer/item-added-to-cart
          ;::customer/id customer-id
          ;::product/id product-id
          ;::product/amount amount))
      ;(after
        ;(command
          ;::customer/place-order
          ;::customer/id customer-id
          ;::order/id order-id))
      ;(expect
        ;(failure
          ;::customer/no-schedule-was-selected)))))
