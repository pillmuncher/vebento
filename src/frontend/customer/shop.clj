(ns frontend.customer.shop
  (:require [clojure.future
             :refer :all]
            [clojure.spec
             :as s]
            [com.stuartsierra.component
             :as co]
            [util
             :refer [ns-alias dummy-uuid]]))


(ns-alias 'entity 'juncture.entity)
(ns-alias 'event 'juncture.event)

(ns-alias 'customer 'vebento.entity.customer)
(ns-alias 'retailer 'vebento.entity.retailer)
(ns-alias 'order 'vebento.entity.order)
(ns-alias 'product 'vebento.entity.product)


(def-command ::load-customer
  :req [::customer/id])


(defn root []
  (atom {:resources {}
         :settings {}
         :customer nil}))


(defrecord Shop

  [dispatcher journal queries root subscriptions]

  co/Lifecycle

  (start [this]
    (assoc
      this :subscriptions
      (subscribe*
        dispatcher

        [::event/type ::load-customer
         (fn [{customer-id ::customer/id}]
           (swap! root
                  assoc :customer
                  (fetch-entity journal ::customer/id customer-id)))]

        [::event/type ::customer/registered
         (fn [{customer-id ::customer/id}]
           (swap! root
                  assoc-in [:customer ::customer/id]
                  customer-id))]

        [::event/type ::customer/address-changed
         (fn [{address ::customer/address}]
           (swap! root
                  assoc-in [:customer ::customer/address]
                  address))]

        [::event/type ::customer/retailer-selected
         (fn [{retailer-id ::retailer/id}]
           (swap! root
                  assoc-in [:customer ::retailer/id]
                  retailer-id))]

        [::event/type ::customer/schedule-selected
         (fn [{schedule ::customer/schedule}]
           (swap! root
                  update-in [:customer ::customer/schedule]
                  union schedule))]

        [::event/type ::customer/payment-method-selected
         (fn [{payment-method ::customer/payment-method}]
           (swap! root
                  assoc-in [:customer ::customer/payment-method]
                  payment-method))]

        [::event/type ::customer/item-added-to-cart
         (fn [{product-id ::product/id
               amount ::product/amount}]
           (swap! root
                  assoc-in [:resources product-id]
                  (run-query queries ::product/by-id :id product-id))
           (swap! root
                  assoc-in [:customer ::customer/cart product-id]
                  amount))]

        [::event/type ::customer/item-removed-from-cart
         (fn [{product-id ::product/id}]
           (swap! root
                  update-in [:customer ::customer/cart]
                  dissoc product-id)
           (swap! root
                  update-in [:resources]
                  dissoc product-id))]

        [::event/type ::order/placed
         (fn [{retailer-id ::retailer/id
               order-id ::order/id
               items ::order/items
               address ::order/address
               schedule ::order/schedule
               payment-method ::order/payment-method}])]

        [::event/type ::customer/cart-cleared
         (fn [_]
           (swap! root
                  assoc-in [:customer ::customer/cart]
                  {}))]

        [::event/type ::entity/not-found
         (fn [{id-key ::entity/key id ::entity/id}])]

        [::event/type ::customer/cart-is-empty
         (fn [_])]

        [::event/type ::customer/has-given-no-address
         (fn [_])]

        [::event/type ::customer/has-selected-no-schedule
         (fn [_])]

        [::event/type ::customer/has-selected-no-payment-method
         (fn [_])]

        [::event/type ::customer/schedule-not-in-retailer-schedule
         (fn [_])]

        [::event/type ::customer/zipcode-not-in-retailer-areas
         (fn [_])]

        [::event/type ::customer/payment-method-not-supported-by-retailer
         (fn [_])]

        [::event/type ::product/not-in-retailer-assortment
         (fn [_])]

        [::event/type ::product/not-in-cart
         (fn [_])])))

  (stop [this]
    (unsubscribe* dispatcher subscriptions)
    (assoc this :subscriptions nil)))


(defn customer-client []
  (-> (co/system-map
        :root
        (root)
        :shop
        (->Shop)
        :dispatcher
        (->WebDispatcher)
        :journal
        (->WebJournal)
        :queries
        (atom {})
        :init
      (co/system-using
        {:dispatcher [:journal]
         :shop [:dispatcher :journal :queries :root]})
      (co/start)))


(def client (customer-client))

(execute-in (client :shop)
            ::customer/place-order
            ::customer/id customer-id)
