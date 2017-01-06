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
(ns-alias 'merchant 'vebento.entity.merchant)
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

        [::load-customer
         (fn [{customer-id ::customer/id}]
           (swap! root
                  assoc :customer
                  (fetch-entity journal ::customer/id customer-id)))]

        [::customer/registered
         (fn [{customer-id ::customer/id}]
           (swap! root
                  assoc-in [:customer ::customer/id]
                  customer-id))]

        [::customer/address-changed
         (fn [{address ::customer/address}]
           (swap! root
                  assoc-in [:customer ::customer/address]
                  address))]

        [::customer/merchant-selected
         (fn [{merchant-id ::merchant/id}]
           (swap! root
                  assoc-in [:customer ::merchant/id]
                  merchant-id))]

        [::customer/schedule-selected
         (fn [{schedule ::customer/schedule}]
           (swap! root
                  update-in [:customer ::customer/schedule]
                  union schedule))]

        [::customer/payment-method-selected
         (fn [{payment-method ::customer/payment-method}]
           (swap! root
                  assoc-in [:customer ::customer/payment-method]
                  payment-method))]

        [::customer/item-added-to-cart
         (fn [{product-id ::product/id
               amount ::product/amount}]
           (swap! root
                  assoc-in [:resources product-id]
                  (run-query queries ::product/by-id :id product-id))
           (swap! root
                  assoc-in [:customer ::customer/cart product-id]
                  amount))]

        [::customer/item-removed-from-cart
         (fn [{product-id ::product/id}]
           (swap! root
                  update-in [:customer ::customer/cart]
                  dissoc product-id)
           (swap! root
                  update-in [:resources]
                  dissoc product-id))]

        [::order/placed
         (fn [{merchant-id ::merchant/id
               order-id ::order/id
               items ::order/items
               address ::order/address
               schedule ::order/schedule
               payment-method ::order/payment-method}])]

        [::customer/cart-cleared
         (fn [_]
           (swap! root
                  assoc-in [:customer ::customer/cart]
                  {}))]

        [::entity/not-found
         (fn [{id-key ::entity/key id ::entity/id}])]

        [::customer/cart-is-empty
         (fn [_])]

        [::customer/has-given-no-address
         (fn [_])]

        [::customer/has-selected-no-schedule
         (fn [_])]

        [::customer/has-selected-no-payment-method
         (fn [_])]

        [::customer/schedule-not-in-merchant-schedule
         (fn [_])]

        [::customer/zipcode-not-in-merchant-areas
         (fn [_])]

        [::merchant/does-not-support-payment-method
         (fn [_])]

        [::product/not-in-merchant-assortment
         (fn [_])]

        [::product/not-in-cart
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
