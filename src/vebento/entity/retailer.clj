(ns vebento.entity.retailer
  (:require [clojure.future
             :refer :all]
            [clojure.spec
             :as s]
            [com.stuartsierra.component
             :as co]
            [juncture.componad
             :as componad
             :refer [within]]
            [juncture.util
             :as j-util
             :refer [ns-alias]]
            [juncture.core
             :as ju
             :refer [def-aggregate aggregate fail-if-exists fail-unless-exists]]
            [juncture.event
             :as event
             :refer [publish def-command def-message subscribe do-unsubscribe
                     store]]
            [juncture.entity
             :as entity
             :refer [create transform def-entity]]
            [vebento.core
             :as vebento]
            [vebento.specs
             :as specs]))


(ns-alias 'customer 'vebento.entity.customer)
(ns-alias 'order 'vebento.entity.order)
(ns-alias 'product 'vebento.entity.product)


(s/def ::id ::specs/id)
(s/def ::address ::specs/address)
(s/def ::areas ::specs/set)
(s/def ::products ::specs/set)
(s/def ::schedule ::specs/set)
(s/def ::customers ::specs/set)
(s/def ::pending-orders ::specs/set)
(s/def ::payment-methods ::specs/set)


(def-command ::register
  :req [::id
        ::address])

(def-command ::add-area
  :req [::id
        ::zipcode])

(def-command ::add-product
  :req [::id
        ::product/id])

(def-command ::add-schedule
  :req [::id
        ::schedule])

(def-command ::add-payment-method
  :req [::id
        ::payment-method])


(def-message ::registered
  :req [::id
        ::address])

(def-message ::area-added
  :req [::id
        ::zipcode])

(def-message ::product-added
  :req [::id
        ::product/id])

(def-message ::schedule-added
  :req [::id
        ::schedule])

(def-message ::payment-method-added
  :req [::id
        ::payment-method])


(def-aggregate ::account)


(def-entity ::entity
  :req [::address
        ::areas
        ::products
        ::schedule
        ::payment-method
        ::customers
        ::pending-orders])


(defmethod transform
  [nil ::registered]
  [_   {retailer-id ::id address ::address}]
  (create ::entity
          ::entity/id retailer-id
          ::address address
          ::areas #{}
          ::products #{}
          ::schedule #{}
          ::payment-method #{}
          ::customers #{}
          ::pending-orders #{}))

(defmethod transform
  [::entity ::area-added]
  [retailer {zipcode ::zipcode}]
  (update retailer ::areas conj zipcode))

(defmethod transform
  [::entity ::product-added]
  [retailer {product-id ::product/id}]
  (update retailer ::products conj product-id))

(defmethod transform
  [::entity ::schedule-added]
  [retailer {schedule ::schedule}]
  (update retailer ::schedule conj schedule))

(defmethod transform
  [::entity ::payment-method-added]
  [retailer {payment-method ::payment-method}]
  (update retailer ::payment-methods conj payment-method))

(defmethod transform
  [::entity ::customer/retailer-selected]
  [retailer {customer-id ::customer/id}]
  (update retailer ::customers conj customer-id))

(defmethod transform
  [::entity ::customer/order-placed]
  [retailer {order-id ::order/id}]
  (update retailer ::customer/pending-orders conj order-id))


(defrecord Component

  [dispatcher event-log subscriptions]

  co/Lifecycle

  (start [this]

    (assoc
      this :subscriptions
      (subscribe

        dispatcher

        [::ju/kind ::ju/message #(store event-log %)]
        [::ju/kind ::ju/failure #(store event-log %)]

        [::ju/type ::register
         (fn [{retailer-id ::id address ::address}]
           (within (aggregate this [::account] retailer-id)
             (fail-if-exists ::id retailer-id)
             (publish ::registered
                      ::id retailer-id
                      ::address address)))]

        [::ju/type ::add-area
         (fn [{retailer-id ::id zipcode ::zipcode}]
           (within (aggregate this [::account] retailer-id)
             (fail-unless-exists ::id retailer-id)
             (publish ::area-added
                      ::id retailer-id
                      ::zipcode zipcode)))]

        [::ju/type ::add-product
         (fn [{retailer-id ::id product-id ::product/id}]
           (within (aggregate this [::account] retailer-id)
             (fail-unless-exists ::id retailer-id)
             (fail-unless-exists ::product/id product-id)
             (publish ::product-added
                      ::id retailer-id
                      ::product/id product-id)))]

        [::ju/type ::add-schedule
         (fn [{retailer-id ::id schedule ::schedule}]
           (within (aggregate this [::account] retailer-id)
             (fail-unless-exists ::id retailer-id)
             (publish ::schedule-added
                      ::id retailer-id
                      ::schedule schedule)))]

        [::ju/type ::add-payment-method
         (fn [{retailer-id ::id payment-method ::payment-method}]
           (within (aggregate this [::account] retailer-id)
             (fail-unless-exists ::id retailer-id)
             (publish ::payment-method-added
                      ::id retailer-id
                      ::payment-method payment-method)))])))

  (stop [this]
    (do-unsubscribe dispatcher subscriptions)
    (assoc this :subscriptions nil)))
