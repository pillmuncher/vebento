(ns frontend.customer.shop
  (:require [clojure.future
             :refer :all]
            [clojure.spec
             :as s]
            [com.stuartsierra.component
             :as co]
            [util
             :refer [ns-alias dummy-uuid]]))


;(ns-alias 'entity 'juncture.entity)
;(ns-alias 'event 'juncture.event)

;(ns-alias 'customer 'vebento.entity.customer)
;(ns-alias 'retailer 'vebento.entity.retailer)
;(ns-alias 'order 'vebento.entity.order)
;(ns-alias 'product 'vebento.entity.product)


;(defrecord Shop

  ;[dispatcher journal database queries app-root subscriptions]

  ;co/Lifecycle

  ;(start [this]
    ;(-> this
        ;(assoc
          ;:app-root
          ;(atom {:resources {}
                 ;:settings {}
                 ;:customer (entity/create
                             ;::customer/entity
                             ;::entity/id dummy-uuid
                             ;::customer/cart {}
                             ;::customer/schedule #{}
                             ;::customer/pending-orders #{})}))
        ;(assoc
          ;:subscriptions
          ;(subscribe*
            ;dispatcher

            ;[::event/kind ::event/command #(store%)]

            ;[::event/type ::customer/registered
             ;(fn [{customer-id ::customer/id}]
               ;(swap! app-root
                      ;assoc-in [:customer ::customer/id]
                      ;customer-id))]

            ;[::event/type ::customer/address-changed
             ;(fn [{address ::customer/address}]
               ;(swap! app-root
                      ;assoc-in [:customer ::customer/address]
                      ;address))]

            ;[::event/type ::customer/retailer-selected
             ;(fn [{retailer-id ::retailer/id}]
               ;(swap! app-root
                      ;assoc-in [:customer ::retailer/id]
                      ;retailer-id))]

            ;[::event/type ::customer/schedule-selected
             ;(fn [{schedule ::customer/schedule}]
               ;(swap! app-root
                      ;update-in [:customer ::customer/schedule]
                      ;union schedule))]

            ;[::event/type ::customer/payment-method-selected
             ;(fn [{payment-method ::customer/payment-method}]
               ;(swap! app-root
                      ;assoc-in [:customer ::customer/payment-method]
                      ;payment-method))]

            ;[::event/type ::customer/item-added-to-cart
             ;(fn [{product-id ::product/id
                   ;amount ::product/amount}]
               ;(swap! app-root
                      ;assoc-in [:resources product-id]
                      ;(run-query queries ::product/by-id :id product-id))
               ;(swap! app-root
                      ;assoc-in [:customer ::customer/cart product-id]
                      ;amount))]

            ;[::event/type ::customer/item-removed-from-cart
             ;(fn [{product-id ::product/id}]
               ;(swap! app-root
                      ;update-in [:customer ::customer/cart]
                      ;dissoc product-id)
               ;(swap! app-root
                      ;update-in [:resources]
                      ;dissoc product-id))]

            ;[::event/type ::order/placed
             ;(fn [{retailer-id ::retailer/id
                   ;order-id ::order/id
                   ;items ::order/items
                   ;address ::order/address
                   ;schedule ::order/schedule
                   ;payment-method ::order/payment-method}])]

            ;[::event/type ::customer/cart-cleared
             ;(fn [_]
               ;(swap! app-root
                      ;assoc-in [:customer ::customer/cart]
                      ;{}))]

            ;[::event/type ::entity/not-found
             ;(fn [{id-key ::entity/key id ::entity/id}])]

            ;[::event/type ::customer/cart-is-empty
             ;(fn [_])]

            ;[::event/type ::customer/has-given-no-address
             ;(fn [_])]

            ;[::event/type ::customer/has-selected-no-schedule
             ;(fn [_])]

            ;[::event/type ::customer/has-selected-no-payment-method
             ;(fn [_])]

            ;[::event/type ::customer/schedule-not-in-retailer-schedule
             ;(fn [_])]

            ;[::event/type ::customer/zipcode-not-in-retailer-areas
             ;(fn [_])]

            ;[::event/type ::customer/payment-method-not-supported-by-retailer
             ;(fn [_])]

            ;[::event/type ::product/not-in-retailer-assortment
             ;(fn [_])]

            ;[::event/type ::product/not-in-cart
             ;(fn [_])]))))

  ;(stop [this]
    ;(unsubscribe* dispatcher subscriptions)
    ;(assoc this :subscriptions nil)))


;(defn customer-client []
  ;(-> (co/system-map
        ;:shop
        ;(->Shop)
        ;:dispatcher
        ;(->SomeEventDispatcher)
        ;:journal
        ;(->SomeJournal)
        ;:database
        ;(->SomeDatabase)
        ;:queries
        ;{})
      ;(co/system-using
        ;{:shop [:dispatcher :journal :database :queries]
         ;...})
      ;(co/start)))


;(def client (customer-client))

;(execute-in (client :shop)
            ;::customer/place-order
            ;::customer/id customer-id)
