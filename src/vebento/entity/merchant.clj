(ns vebento.entity.merchant
  (:require [clojure.future
             :refer :all]
            [clojure.set
             :refer [union]]
            [clojure.spec
             :as s]
            [com.stuartsierra.component
             :as co]
            [util
             :refer [ns-alias]]
            [juncture.event
             :as event
             :refer [def-command def-message def-failure def-event
                     subscribe* unsubscribe*]]
            [juncture.entity
             :as entity
             :refer [register unregister upgrade-entity
                     def-entity create transform]]
            [componad
             :refer [within]]
            [vebento.core
             :refer [aggregate publish execute fail-with
                     fail-if-exists fail-unless-exists]]))


(ns-alias 'specs 'vebento.specs)
(ns-alias 'customer 'vebento.entity.customer)
(ns-alias 'order 'vebento.entity.order)
(ns-alias 'product 'vebento.entity.product)


(s/def ::id ::specs/id)
(s/def ::address ::specs/address)
(s/def ::areas ::specs/set)
(s/def ::products ::specs/set)
(s/def ::schedule ::specs/schedule)
(s/def ::customers ::specs/set)
(s/def ::pending-orders ::specs/set)
(s/def ::payment-method ::specs/payment-method)
(s/def ::payment-methods ::specs/set)
(s/def ::schedule-time-of-day string?)
(s/def ::schedule-is-recurrent string?)


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


(def-failure ::does-not-support-payment-method
  :req [::id
        ::payment-method])


(def-entity ::entity
  :req [::address
        ::areas
        ::products
        ::schedule
        ::payment-methods
        ::customers
        ::pending-orders])


(defmethod transform
  [nil ::registered]
  [_   {merchant-id ::id address ::address}]
  (create ::entity
          ::entity/id merchant-id
          ::address address
          ::areas #{}
          ::products #{}
          ::schedule #{}
          ::payment-methods #{}
          ::customers #{}
          ::pending-orders #{}))

(defmethod transform
  [::entity ::area-added]
  [merchant {zipcode ::zipcode}]
  (update merchant ::areas conj zipcode))

(defmethod transform
  [::entity ::product-added]
  [merchant {product-id ::product/id}]
  (update merchant ::products conj product-id))

(defmethod transform
  [::entity ::schedule-added]
  [merchant {schedule ::schedule}]
  (update merchant ::schedule union schedule))

(defmethod transform
  [::entity ::payment-method-added]
  [merchant {payment-method ::payment-method}]
  (update merchant ::payment-methods conj payment-method))

(defmethod transform
  [::entity ::customer/merchant-selected]
  [merchant {customer-id ::customer/id}]
  (update merchant ::customers conj customer-id))

(defmethod transform
  [::entity ::order/placed]
  [merchant {order-id ::order/id}]
  (update merchant ::customer/pending-orders conj order-id))


(defrecord Component

  [aggregates dispatcher journal entity-store subscriptions]

  co/Lifecycle

  (start [this]

    (register aggregates [::account])

    (assoc
      this :subscriptions
      (subscribe*

        dispatcher

        [::event/type ::registered
         (upgrade-entity entity-store ::id)]

        [::event/type ::area-added
         (upgrade-entity entity-store ::id)]

        [::event/type ::product-added
         (upgrade-entity entity-store ::id)]

        [::event/type ::schedule-added
         (upgrade-entity entity-store ::id)]

        [::event/type ::payment-method-added
         (upgrade-entity entity-store ::id)]

        [::event/type ::customer/merchant-selected
         (upgrade-entity entity-store ::id)]

        [::event/type ::order/placed
         (upgrade-entity entity-store ::id)]

        [::event/type ::register
         (fn [{merchant-id ::id address ::address}]
           (within (aggregate this [::account] merchant-id)
             (fail-if-exists ::id merchant-id)
             (publish ::registered
                      ::id merchant-id
                      ::address address)))]

        [::event/type ::add-area
         (fn [{merchant-id ::id zipcode ::zipcode}]
           (within (aggregate this [::account] merchant-id)
             (fail-unless-exists ::id merchant-id)
             (publish ::area-added
                      ::id merchant-id
                      ::zipcode zipcode)))]

        [::event/type ::add-product
         (fn [{merchant-id ::id product-id ::product/id}]
           (within (aggregate this [::account] merchant-id)
             (fail-unless-exists ::id merchant-id)
             (fail-unless-exists ::product/id product-id)
             (publish ::product-added
                      ::id merchant-id
                      ::product/id product-id)))]

        [::event/type ::add-schedule
         (fn [{merchant-id ::id schedule ::schedule}]
           (within (aggregate this [::account] merchant-id)
             (fail-unless-exists ::id merchant-id)
             (publish ::schedule-added
                      ::id merchant-id
                      ::schedule schedule)))]

        [::event/type ::add-payment-method
         (fn [{merchant-id ::id payment-method ::payment-method}]
           (within (aggregate this [::account] merchant-id)
             (fail-unless-exists ::id merchant-id)
             (publish ::payment-method-added
                      ::id merchant-id
                      ::payment-method payment-method)))])))

  (stop [this]
    (unregister aggregates [::account])
    (apply unsubscribe* dispatcher subscriptions)
    (assoc this :subscriptions nil)))
