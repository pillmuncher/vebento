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
             :refer [execute publish fail-with]]
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
        :customer
        (customer/->Component nil nil nil)
        :retailer
        (retailer/->Component nil nil nil)
        :dispatcher
        (mock-dispatcher)
        :journal
        (mock-journal))
      (co/system-using
        {:customer [:journal :dispatcher]
         :retailer [:journal :dispatcher]
         :dispatcher [:journal]})
      (co/start)))


;(clojure.test/run-tests)


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
        (execute
          ::customer/register
          ::customer/id customer-id))
      (expect
        (publish
          ::customer/registered
          ::customer/id customer-id))))

  (def-scenario customer-gets-registered-with-address
    (within (system (test-bench))
      (after
        (execute
          ::customer/register
          ::customer/id customer-id
          ::customer/address customer-address))
      (expect
        (publish
          ::customer/registered
          ::customer/id customer-id)
        (publish
          ::customer/address-changed
          ::customer/id customer-id
          ::customer/address customer-address))))

  (def-scenario customer-can-register-only-once
    (within (system (test-bench))
      (given
        (publish
          ::customer/registered
          ::customer/id customer-id))
      (after
        (execute
          ::customer/register
          ::customer/id customer-id))
      (expect
        (fail-with
          ::entity/already-exists
          ::entity/id-key ::customer/id
          ::entity/id customer-id))))

  (def-scenario customer-cart-gets-cleared
    (within (system (test-bench))
      (given
        (publish
          ::customer/registered
          ::customer/id customer-id))
      (after
        (execute
          ::customer/clear-cart
          ::customer/id customer-id))
      (expect
        (publish
          ::customer/cart-cleared
          ::customer/id customer-id))))

  (def-scenario customer-cart-can-only-be-cleared-if-customer-exists
    (within (system (test-bench))
      (after
        (execute
          ::customer/clear-cart
          ::customer/id customer-id))
      (expect
        (fail-with
          ::entity/not-found
          ::entity/id-key ::customer/id
          ::entity/id customer-id))))

  (def-scenario retailer-gets-selected
    (within (system (test-bench))
      (given
        (publish
          ::retailer/registered
          ::retailer/id retailer-id
          ::retailer/address retailer-address)
        (publish
          ::retailer/area-added
          ::retailer/id retailer-id
          ::retailer/zipcode (::customer/zipcode customer-address))
        (publish
          ::customer/registered
          ::customer/id customer-id)
        (publish
          ::customer/address-changed
          ::customer/id customer-id
          ::customer/address customer-address))
      (after
        (execute
          ::customer/select-retailer
          ::customer/id customer-id
          ::retailer/id retailer-id))
      (expect
        (publish
          ::customer/retailer-selected
          ::customer/id customer-id
          ::retailer/id retailer-id)))))

  ;(def-scenario retailer-can-only-be-selected-if-customer-exists
    ;(within (system (test-bench))
      ;(after
        ;(execute
          ;::customer/select-retailer
          ;::customer/id customer-id
          ;::retailer/id retailer-id))
      ;(expect
        ;(fail-with
          ;::entity/not-found
          ;::entity/id-key ::customer/id
          ;::entity/id customer-id))))

  ;(def-scenario select-retailer-fails-unless-address-was-given
    ;(within (system (test-bench))
      ;(given
        ;(publish
          ;::retailer/registered
          ;::retailer/id retailer-id
          ;::retailer/address retailer-address)
        ;(publish
          ;::retailer/area-added
          ;::retailer/id retailer-id
          ;::retailer/zipcode (:zipcode customer-address))
        ;(publish
          ;::customer/registered
          ;::customer/id customer-id))
      ;(after
        ;(execute
          ;::customer/select-retailer
          ;::customer/id customer-id
          ;::retailer/id retailer-id))
      ;(expect
        ;(fail-with
          ;::customer/has-given-no-address
          ;::customer/id customer-id))))

  ;(def-scenario select-retailer-fails-unless-he-delivers-in-the-customers-area
    ;(within (system (test-bench))
      ;(given
        ;(publish
          ;::retailer/registered
          ;::retailer/id retailer-id
          ;::retailer/address retailer-address)
        ;(publish
          ;::customer/registered
          ;::customer/id customer-id
          ;::customer/address customer-address))
      ;(after
        ;(execute
          ;::customer/select-retailer
          ;::customer/id customer-id
          ;::retailer/id retailer-id))
      ;(expect
        ;(fail-with
          ;::customer/zipcode-not-in-retailer-areas
          ;::customer/id customer-id
          ;::customer/zipcode (::customer/zipcode customer-address)))))

  ;(def-scenario item-gets-added-to-customers-cart
    ;(within (system (test-bench))
      ;(given
        ;(publish
          ;::retailer/registered
          ;::retailer/id retailer-id
          ;::retailer/address retailer-address)
        ;(publish
          ;::retailer/product-added
          ;::retailer/id retailer-id
          ;::product/id product-id)
        ;(publish
          ;::customer/registered
          ;::customer/id customer-id
          ;::customer/address customer-address)
        ;(publish
          ;::customer/retailer-selected
          ;::customer/id customer-id
          ;::retailer/id retailer-id))
      ;(after
        ;(execute
          ;::customer/add-item-to-cart
          ;::customer/id customer-id
          ;::product/id product-id
          ;::product/amount amount))
      ;(expect
        ;(publish
          ;::customer/item-added-to-cart
          ;::customer/id customer-id
          ;::product/id product-id
          ;::product/amount amount))))

  ;(def-scenario add-item-fails-unless-retailer-sells-the-selected-product
    ;(within (system (test-bench))
      ;(given
        ;(publish
          ;::retailer/registered
          ;::retailer/id retailer-id
          ;::retailer/address retailer-address)
        ;(publish
          ;::customer/registered
          ;::customer/id customer-id
          ;::customer/address customer-address)
        ;(publish
          ;::customer/retailer-selected
          ;::customer/id customer-id
          ;::retailer/id retailer-id))
      ;(after
        ;(execute
          ;::customer/add-item-to-cart
          ;::customer/id customer-id
          ;::product/id product-id
          ;::product/amount amount))
      ;(expect
        ;(fail-with
          ;::customer/product-not-in-retailer-assortment
          ;::customer/id customer-id
          ;::product/id product-id))))

  ;(def-scenario customer-order-gets-placed
    ;(within (system (test-bench))
      ;(given
        ;(publish
          ;::retailer/registered
          ;::retailer/id retailer-id
          ;::retailer/address retailer-address)
        ;(publish
          ;::retailer/schedule-added
          ;::retailer/id retailer-id
          ;::retailer/schedule schedule)
        ;(publish
          ;::customer/registered
          ;::customer/id customer-id
          ;::customer/address customer-address
          ;::retailer/id retailer-id)
        ;(publish
          ;::customer/schedule-selected
          ;::customer/id customer-id
          ;::customer/schedule schedule)
        ;(publish
          ;::customer/item-added-to-cart
          ;::customer/id customer-id
          ;::product/id product-id
          ;::product/amount amount))
      ;(after
        ;(execute
          ;::customer/place-order
          ;::customer/id customer-id
          ;::order/id order-id))
      ;(expect
        ;(publish
          ;::customer/order-placed
          ;::customer/id customer-id
          ;::retailer/id retailer-id
          ;::order/id order-id
          ;::order/address customer-address
          ;::order/schedule schedule
          ;::order/items {::product/id product-id
                         ;::product/amount amount})
        ;(publish
          ;::customer/cart-cleared
          ;::customer/id customer-id)))))

  ;(def-scenario place-customer-order-fails-when-cart-is-empty
    ;(within (system (test-bench))
      ;(given
        ;(publish
          ;::retailer/registered
          ;::retailer/id retailer-id
          ;::retailer/address retailer-address)
        ;(publish
          ;::retailer/schedule-added
          ;::retailer/id retailer-id
          ;::retailer/schedule schedule)
        ;(publish
          ;::customer/registered
          ;::customer/id customer-id
          ;::customer/address customer-address)
        ;(publish
          ;::customer/retailer-selected
          ;::customer/id customer-id
          ;::retailer/id retailer-id)
        ;(publish
          ;::customer/schedule-selected
          ;::customer/id customer-id
          ;::customer/schedule schedule)
        ;(after
          ;(execute
            ;::customer/place-order
            ;::customer/id customer-id
            ;::order/id order-id))
        ;(expect
          ;(fail-with
            ;::customer/cart-is-empty)))))

  ;(def-scenario place-customer-order-fails-unless-address-was-provided
    ;(within (system (test-bench))
      ;(given
        ;(publish
          ;::retailer/registered
          ;::retailer/id retailer-id
          ;::retailer/address retailer-address)
        ;(publish
          ;::retailer/schedule-added
          ;::retailer/id retailer-id
          ;::retailer/schedule schedule)
        ;(publish
          ;::customer/registered
          ;::customer/id customer-id
          ;::customer/address nil)
        ;(publish
          ;::customer/retailer-selected
          ;::customer/id customer-id
          ;::retailer/id retailer-id)
        ;(publish
          ;::customer/schedule-selected
          ;::customer/id customer-id
          ;::customer/schedule schedule)
        ;(publish
          ;::customer/item-added-to-cart
          ;::customer/id customer-id
          ;::product/id product-id
          ;::product/amount amount))
      ;(after
        ;(execute
          ;::customer/place-order
          ;::customer/id customer-id
          ;::order/id order-id))
      ;(expect
        ;(fail-with
          ;::customer/no-address-was-provided))))

  ;(def-scenario place-customer-order-fails-unless-retailer-was-selected
    ;(within (system (test-bench))
      ;(given
        ;(publish
          ;::retailer/registered
          ;::retailer/id retailer-id
          ;::retailer/address retailer-address)
        ;(publish
          ;::retailer/schedule-added
          ;::retailer/id retailer-id
          ;::retailer/schedule schedule)
        ;(publish
          ;::customer/registered
          ;::customer/id customer-id
          ;::customer/address customer-address)
        ;(publish
          ;::customer/schedule-selected
          ;::customer/id customer-id
          ;::customer/schedule schedule)
        ;(publish
          ;::customer/item-added-to-cart
          ;::customer/id customer-id
          ;::product/id product-id
          ;::product/amount amount))
      ;(after
        ;(execute
          ;::customer/place-order
          ;::customer/id customer-id
          ;::order/id order-id))
      ;(expect
        ;(fail-with
          ;::customer/no-retailer-was-selected))))

  ;(def-scenario place-customer-order-fails-unless-schedule-was-selected
    ;(within (system (test-bench))
      ;(given
        ;(publish
          ;::retailer/registered
          ;::retailer/id retailer-id
          ;::retailer/address retailer-address)
        ;(publish
          ;::retailer/schedule-added
          ;::retailer/id retailer-id
          ;::retailer/schedule schedule)
        ;(publish
          ;::customer/registered
          ;::customer/id customer-id
          ;::customer/address customer-address)
        ;(publish
          ;::customer/retailer-selected
          ;::customer/id customer-id
          ;::retailer/id retailer-id)
        ;(publish
          ;::customer/item-added-to-cart
          ;::customer/id customer-id
          ;::product/id product-id
          ;::product/amount amount))
      ;(after
        ;(execute
          ;::customer/place-order
          ;::customer/id customer-id
          ;::order/id order-id))
      ;(expect
        ;(fail-with
          ;::customer/no-schedule-was-selected)))))
