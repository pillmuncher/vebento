(ns vebento.entity.customer
  (:require [clojure.future
             :refer :all]
            [clojure.set
             :refer [intersection]]
            [clojure.spec
             :as s]
            [com.stuartsierra.component
             :as co]
            [monads.util
             :refer [mwhen]]
            [util
             :refer [ns-alias not-in?]]
            [juncture.event
             :as event
             :refer [def-command def-message def-failure
                     subscribe* unsubscribe*]]
            [juncture.entity
             :as entity
             :refer [register unregister
                     def-entity create transform]]
            [componad
             :refer [within]]
            [vebento.core
             :refer [aggregate publish execute fail-with
                     fail-if-exists fail-unless-exists f-mwhen get-entity]]
            [vebento.specs
             :as specs]))


(ns-alias 'order 'vebento.entity.order)
(ns-alias 'product 'vebento.entity.product)
(ns-alias 'retailer 'vebento.entity.retailer)


(s/def ::id ::specs/id)
(s/def ::address ::specs/address)
(s/def ::cart ::specs/cart)
(s/def ::pending-orders ::specs/set)
(s/def ::schedule ::specs/schedule)
(s/def ::payment-method ::specs/payment-method)


(def-command ::register
  :req [::id]
  :opt [::address
        ::retailer/id
        ::payment-method])

(def-command ::change-address
  :req [::id
        ::address])

(def-command ::select-retailer
  :req [::id
        ::retailer/id])

(def-command ::select-schedule
  :req [::id
        ::schedule])

(def-command ::select-payment-method
  :req [::id
        ::payment-method])

(def-command ::add-item-to-cart
  :req [::id
        ::product/id
        ::product/amount])

(def-command ::remove-item-from-cart
  :req [::id
        ::product/id])

(def-command ::place-order
  :req [::id
        ::order/id])

(def-command ::clear-cart
  :req [::id])


(def-message ::registered
  :req [::id])

(def-message ::address-changed
  :req [::id
        ::address])

(def-message ::retailer-selected
  :req [::id
        ::retailer/id])

(def-message ::schedule-selected
  :req [::id
        ::schedule])

(def-message ::payment-method-selected
  :req [::id
        ::payment-method])

(def-message ::item-added-to-cart
  :req [::id
        ::product/id
        ::product/amount])

(def-message ::item-removed-from-cart
  :req [::id
        ::product/id])

(def-message ::cart-cleared
  :req [::id])


(def-failure ::cart-is-empty
  :req [::id])

(def-failure ::has-given-no-address
  :req [::id])

(def-failure ::has-selected-no-retailer
  :req [::id])

(def-failure ::has-selected-no-schedule
  :req [::id])

(def-failure ::has-selected-no-payment-method
  :req [::id])

(def-failure ::zipcode-not-in-retailer-areas
  :req [::id
        ::zipcode])

(def-failure ::schedule-not-in-retailer-schedule
  :req [::id
        ::schedule])

(def-failure ::payment-method-not-supported-by-retailer
  :req [::id
        ::payment-method])

(def-failure ::product-not-in-retailer-assortment
  :req [::id
        ::product/id])

(def-failure ::product-not-in-cart
  :req [::id
        ::product/id])


(def-entity ::entity
  :req [::cart
        ::pending-orders]
  :opt [::address
        ::retailer/id
        ::schedule
        ::payment-method])


(defmethod transform
  [nil ::registered]
  [_   {customer-id ::id}]
  (create ::entity
          ::entity/id customer-id
          ::cart {}
          ::pending-orders #{}))

(defmethod transform
  [::entity ::address-changed]
  [customer {address ::address}]
  (assoc customer ::address address))

(defmethod transform
  [::entity ::retailer-selected]
  [customer {retailer-id ::retailer/id}]
  (assoc customer ::retailer/id retailer-id))

(defmethod transform
  [::entity ::schedule-selected]
  [customer {schedule ::schedule}]
  (assoc customer ::schedule schedule))

(defmethod transform
  [::entity ::payment-method-selected]
  [customer {payment-method ::payment-method}]
  (assoc customer ::payment-method payment-method))

(defmethod transform
  [::entity ::item-added-to-cart]
  [customer {product-id ::product/id amount ::product/amount}]
  (if (zero? amount)
    (update customer ::cart dissoc product-id)
    (assoc-in customer [::cart product-id] amount)))

(defmethod transform
  [::entity ::item-removed-from-cart]
  [customer {product-id ::product/id}]
  (update customer ::cart dissoc product-id))

(defmethod transform
  [::entity ::order/placed]
  [customer {order-id ::order/id}]
  (update customer ::pending-orders conj order-id))

(defmethod transform
  [::entity ::cart-cleared]
  [customer _]
  (assoc customer ::cart {}))


(defrecord Component

  [aggregates dispatcher journal subscriptions]

  co/Lifecycle

  (start [this]

    (register aggregates [::account ::shopping])

    (assoc
      this :subscriptions
      (subscribe*

        dispatcher

        [::event/type ::register
         (fn [{customer-id ::id
               address ::address
               retailer-id ::retailer/id
               payment-method ::payment-method}]
           (within (aggregate this [::account] customer-id)
             (fail-if-exists ::id customer-id)
             (publish ::registered
                      ::id customer-id)
             (fail-unless-exists ::id customer-id)
             (mwhen (some? address)
                    (execute ::change-address
                             ::id customer-id
                             ::address address))
             (mwhen (some? retailer-id)
                    (execute ::select-retailer
                             ::id customer-id
                             ::retailer/id retailer-id))
             (mwhen (some? payment-method)
                    (execute ::select-payment-method
                             ::id customer-id
                             ::payment-method payment-method))))]

        [::event/type ::change-address
         (fn [{customer-id ::id
               address ::address}]
           (within (aggregate this [::account ::shopping] customer-id)
             (fail-unless-exists ::id customer-id)
             (publish ::address-changed
                      ::id customer-id
                      ::address address)))]

        [::event/type ::select-retailer
         (fn [{customer-id ::id
               retailer-id ::retailer/id}]
           (within (aggregate this [::shopping] customer-id)
             customer <- (get-entity ::id customer-id)
             (mwhen (-> @customer ::address nil?)
                    (fail-with ::has-given-no-address
                               ::id customer-id))
             retailer <- (get-entity ::retailer/id retailer-id)
             (mwhen (->> @customer ::address ::zipcode
                         (not-in? (@retailer ::retailer/areas)))
                    (fail-with ::zipcode-not-in-retailer-areas
                               ::id customer-id
                               ::zipcode (@customer ::zipcode)))
             (publish ::retailer-selected
                      ::id customer-id
                      ::retailer/id retailer-id)))]

        [::event/type ::select-schedule
         (fn [{customer-id ::id
               schedule ::schedule}]
           (within (aggregate this [::shopping] customer-id)
             customer <- (get-entity ::id customer-id)
             retailer <- (get-entity ::retailer/id (@customer ::retailer/id))
             (mwhen (->> @customer ::schedule
                         (intersection (@retailer ::retailer/schedule))
                         empty?)
                    (fail-with ::schedule-not-in-retailer-schedule
                               ::id customer-id
                               ::schedule schedule))
             (publish ::schedule-selected
                      ::id customer-id
                      ::schedule schedule)))]

        [::event/type ::select-payment-method
         (fn [{customer-id ::id
               payment-method ::payment-method}]
           (within (aggregate this [::shopping] customer-id)
             customer <- (get-entity ::id customer-id)
             retailer <- (get-entity ::retailer/id (@customer ::retailer/id))
             (mwhen (->> @customer ::payment-method
                         (not-in? (@retailer ::retailer/payment-methods)))
                    (fail-with ::payment-method-not-supported-by-retailer
                               ::id customer-id
                               ::payment-method (@customer ::payment-method)))
             (publish ::payment-method-selected
                      ::id customer-id
                      ::payment-method payment-method)))]

        [::event/type ::add-item-to-cart
         (fn [{customer-id ::id
               product-id ::product/id
               amount ::product/amount}]
           (within (aggregate this [::shopping] customer-id)
             customer <- (get-entity ::id customer-id)
             retailer <- (get-entity ::retailer/id (@customer ::retailer/id))
             (mwhen (->> product-id
                         (not-in? (@retailer ::retailer/products)))
                    (fail-with ::product-not-in-retailer-assortment
                               ::id customer-id
                               ::product/id product-id))
             (publish ::item-added-to-cart
                      ::id customer-id
                      ::product/id product-id
                      ::product/amount amount)))]

        [::event/type ::remove-item-from-cart
         (fn [{customer-id ::id
               product-id ::product/id}]
           (within (aggregate this [::shopping] customer-id)
             customer <- (get-entity ::id customer-id)
             (mwhen (->> product-id
                         (not-in? (@customer ::cart)))
                    (fail-with ::product-not-in-cart
                               ::id customer-id
                               ::product/id product-id))
             (publish ::item-removed-from-cart
                      ::id customer-id
                      ::product/id product-id)))]

        [::event/type ::place-order
         (fn [{customer-id ::id
               order-id ::order/id}]
           (within (aggregate this [::shopping] customer-id)
             (fail-if-exists ::order/id order-id)
             customer <- (get-entity ::id customer-id)
             (f-mwhen (-> @customer ::cart empty?)
                      (fail-with ::cart-is-empty
                                 ::id customer-id)
                      (-> @customer ::retailer/id nil?)
                      (fail-with ::has-selected-no-retailer
                                 ::id customer-id)
                      (-> @customer ::address nil?)
                      (fail-with ::has-given-no-address
                                 ::id customer-id)
                      (-> @customer ::schedule nil?)
                      (fail-with ::has-selected-no-schedule
                                 ::id customer-id)
                      (-> @customer ::payment-method nil?)
                      (fail-with ::has-selected-no-payment-method
                                 ::id customer-id))
             retailer <- (get-entity ::retailer/id (@customer ::retailer/id))
             (f-mwhen (->> @customer ::schedule
                           (intersection (@retailer ::retailer/schedule))
                           empty?)
                      (fail-with ::schedule-not-in-retailer-schedule
                                 ::id customer-id
                                 ::schedule (@customer ::schedule))
                      (->> @customer ::payment-method
                           (not-in? (@retailer ::retailer/payment-methods)))
                      (fail-with ::payment-method-not-supported-by-retailer
                                 ::id customer-id
                                 ::payment-method (@customer ::payment-method))
                      (->> @customer ::address ::specs/zipcode
                           (not-in? (@retailer ::retailer/areas)))
                      (fail-with ::zipcode-not-in-retailer-areas
                                 ::id customer-id
                                 ::specs/zipcode (-> @customer
                                                     ::address
                                                     ::specs/zipcode)))
             (publish ::order/placed
                      ::id customer-id
                      ::retailer/id (@customer ::retailer/id)
                      ::order/id order-id
                      ::order/items (@customer ::cart)
                      ::order/address (@customer ::address)
                      ::order/payment-method (@customer ::payment-method)
                      ::order/schedule (intersection
                                         (@customer ::schedule)
                                         (@retailer ::retailer/schedule)))
             (publish ::cart-cleared
                      ::id customer-id)))]

        [::event/type ::clear-cart
         (fn [{customer-id ::id}]
           (within (aggregate this [::shopping] customer-id)
             (fail-unless-exists ::id customer-id)
             (publish ::cart-cleared
                      ::id customer-id)))])))

(stop [this]
      (unregister aggregates [::account ::shopping])
      (apply unsubscribe* dispatcher subscriptions)
      (assoc this :subscriptions nil)))
