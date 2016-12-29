(ns vebento.entity.customer
  (:require [clojure.future
             :refer :all]
            [clojure.set
             :refer [union intersection]]
            [clojure.spec
             :as s]
            [com.stuartsierra.component
             :as co]
            [monads.core
             :refer [return]]
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
             :refer [register unregister upgrade-entity
                     def-entity create transform]]
            [componad
             :refer [within mdo-await*]]
            [vebento.core
             :refer [aggregate publish execute fail-with
                     fail-if-exists fail-unless-exists get-entity]]))


(ns-alias 'specs 'vebento.specs)
(ns-alias 'order 'vebento.entity.order)
(ns-alias 'product 'vebento.entity.product)
(ns-alias 'merchant 'vebento.entity.merchant)


(s/def ::id ::specs/id)
(s/def ::address ::specs/address)
(s/def ::cart ::specs/cart)
(s/def ::pending-orders ::specs/set)
(s/def ::schedule ::specs/schedule)
(s/def ::payment-method ::specs/payment-method)


(def-command ::register
  :req [::id]
  :opt [::address
        ::merchant/id
        ::payment-method])

(def-command ::change-address
  :req [::id
        ::address])

(def-command ::select-merchant
  :req [::id
        ::merchant/id])

(def-command ::add-schedule
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

(def-message ::merchant-selected
  :req [::id
        ::merchant/id])

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

(def-failure ::has-selected-no-merchant
  :req [::id])

(def-failure ::has-selected-no-schedule
  :req [::id])

(def-failure ::has-selected-no-payment-method
  :req [::id])

(def-failure ::zipcode-not-in-merchant-areas
  :req [::id
        ::zipcode])

(def-failure ::schedule-not-in-merchant-schedule
  :req [::id
        ::schedule])

(def-failure ::product-not-in-merchant-assortment
  :req [::id
        ::product/id])

(def-failure ::product-not-in-cart
  :req [::id
        ::product/id])


(def-entity ::entity
  :req [::cart
        ::schedule
        ::pending-orders]
  :opt [::address
        ::merchant/id
        ::payment-method])


(defmethod transform
  [nil ::registered]
  [_   {customer-id ::id}]
  (create ::entity
          ::entity/id customer-id
          ::cart {}
          ::schedule #{}
          ::pending-orders #{}))

(defmethod transform
  [::entity ::address-changed]
  [customer {address ::address}]
  (assoc customer ::address address))

(defmethod transform
  [::entity ::merchant-selected]
  [customer {merchant-id ::merchant/id}]
  (assoc customer ::merchant/id merchant-id))

(defmethod transform
  [::entity ::schedule-selected]
  [customer {schedule ::schedule}]
  (update customer ::schedule union schedule))

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

  [aggregates dispatcher journal entity-store subscriptions]

  co/Lifecycle

  (start [this]

    (register aggregates [::account ::shopping])

    (assoc
      this :subscriptions
      (subscribe*

        dispatcher

        [::event/type ::registered
         (upgrade-entity entity-store ::id)]

        [::event/type ::address-changed
         (upgrade-entity entity-store ::id)]

        [::event/type ::merchant-selected
         (upgrade-entity entity-store ::id)]

        [::event/type ::schedule-selected
         (upgrade-entity entity-store ::id)]

        [::event/type ::payment-method-selected
         (upgrade-entity entity-store ::id)]

        [::event/type ::item-added-to-cart
         (upgrade-entity entity-store ::id)]

        [::event/type ::item-removed-from-cart
         (upgrade-entity entity-store ::id)]

        [::event/type ::cart-cleared
         (upgrade-entity entity-store ::id)]

        [::event/type ::order/placed
         (upgrade-entity entity-store ::id)]

        [::event/type ::register
         (fn [{customer-id ::id
               address ::address
               merchant-id ::merchant/id
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
             (mwhen (some? merchant-id)
                    (execute ::select-merchant
                             ::id customer-id
                             ::merchant/id merchant-id))
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

        [::event/type ::select-merchant
         (fn [{customer-id ::id
               merchant-id ::merchant/id}]
           (within (aggregate this [::shopping] customer-id)
             customer <- (get-entity ::id customer-id)
             (mwhen (-> @customer ::address nil?)
                    (fail-with ::has-given-no-address
                               ::id customer-id))
             merchant <- (get-entity ::merchant/id merchant-id)
             (mwhen (->> @customer ::address ::specs/zipcode
                         (not-in? (@merchant ::merchant/areas)))
                    (fail-with ::zipcode-not-in-merchant-areas
                               ::id customer-id
                               ::zipcode (-> @customer
                                             ::address
                                             ::specs/zipcode)))
             (publish ::merchant-selected
                      ::id customer-id
                      ::merchant/id merchant-id)))]

        [::event/type ::add-schedule
         (fn [{customer-id ::id
               schedule ::schedule}]
           (within (aggregate this [::shopping] customer-id)
             customer <- (get-entity ::id customer-id)
             merchant <- (get-entity ::merchant/id (@customer ::merchant/id))
             (mwhen (empty? (intersection schedule
                                          (@merchant ::merchant/schedule)))
                    (fail-with ::schedule-not-in-merchant-schedule
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
             merchant <- (get-entity ::merchant/id (@customer ::merchant/id))
             (mwhen (->> payment-method
                         (not-in? (@merchant ::merchant/payment-methods)))
                    (fail-with ::does-not-support-payment-method
                               ::id customer-id
                               ::payment-method payment-method))
             (publish ::payment-method-selected
                      ::id customer-id
                      ::payment-method payment-method)))]

        [::event/type ::add-item-to-cart
         (fn [{customer-id ::id
               product-id ::product/id
               amount ::product/amount}]
           (within (aggregate this [::shopping] customer-id)
             customer <- (get-entity ::id customer-id)
             merchant <- (get-entity ::merchant/id (@customer ::merchant/id))
             (mwhen (->> product-id
                         (not-in? (@merchant ::merchant/products)))
                    (fail-with ::product-not-in-merchant-assortment
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
             (mdo-await*
               (mwhen (-> @customer ::cart empty?)
                      (fail-with ::cart-is-empty
                                 ::id customer-id))
               (mwhen (-> @customer ::merchant/id nil?)
                      (fail-with ::has-selected-no-merchant
                                 ::id customer-id))
               (mwhen (-> @customer ::address nil?)
                      (fail-with ::has-given-no-address
                                 ::id customer-id))
               (mwhen (-> @customer ::schedule empty?)
                      (fail-with ::has-selected-no-schedule
                                 ::id customer-id))
               (mwhen (-> @customer ::payment-method nil?)
                      (fail-with ::has-selected-no-payment-method
                                 ::id customer-id)))
             merchant <- (get-entity ::merchant/id (@customer ::merchant/id))
             (mdo-await*
               (mwhen (empty? (intersection (@customer ::schedule)
                                            (@merchant ::merchant/schedule)))
                      (fail-with ::schedule-not-in-merchant-schedule
                                 ::id customer-id
                                 ::schedule (@customer ::schedule)))
               (mwhen (->> @customer ::payment-method
                           (not-in? (@merchant ::merchant/payment-methods)))
                      (fail-with ::merchant/does-not-support-payment-method
                                 ::id customer-id
                                 ::payment-method (@customer ::payment-method)))
               (mwhen (->> @customer ::address ::specs/zipcode
                           (not-in? (@merchant ::merchant/areas)))
                      (fail-with ::zipcode-not-in-merchant-areas
                                 ::id customer-id
                                 ::specs/zipcode (-> @customer
                                                     ::address
                                                     ::specs/zipcode))))
             (publish ::order/placed
                      ::id customer-id
                      ::merchant/id (@customer ::merchant/id)
                      ::order/id order-id
                      ::order/items (@customer ::cart)
                      ::order/address (@customer ::address)
                      ::order/payment-method (@customer ::payment-method)
                      ::order/schedule (intersection
                                         (@customer ::schedule)
                                         (@merchant ::merchant/schedule)))
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
