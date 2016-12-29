(ns vebento.entity.product
  (:require [clojure.future
             :refer :all]
            [clojure.spec
             :as s]
            [com.stuartsierra.component
             :as co]
            [util
             :refer [ns-alias]]
            [juncture.event
             :as event
             :refer [def-command def-message def-failure
                     subscribe* unsubscribe*]]
            [juncture.entity
             :as entity
             :refer [register unregister def-entity create transform]]
            [componad
             :refer [within]]
            [vebento.core
             :refer [aggregate publish execute fail-with fail-if-exists
                     fail-unless-exists update-entity get-entity]]))


(ns-alias 'specs 'vebento.specs)
(ns-alias 'customer 'vebento.entity.customer)
(ns-alias 'merchant 'vebento.entity.merchant)
(ns-alias 'order 'vebento.entity.order)


(s/def ::id ::specs/id)
(s/def ::name ::specs/name)
(s/def ::amount ::specs/amount)


(def-command ::create
  :req [::id
        ::name])


(def-message ::created
  :req [::id
        ::name])


(def-entity ::entity
  :req [::name])


(defmethod transform
  [nil ::created]
  [_ {product-id ::id
      name ::name}]
  (create ::entity
          ::entity/id product-id
          ::name name))


(defrecord Component

  [aggregates dispatcher journal entity-store subscriptions]

  co/Lifecycle

  (start [this]

    (register aggregates [::assortment])

    (assoc
      this :subscriptions
      (subscribe*
        dispatcher

        [::event/type ::created
         (update-entity entity-store ::id)]

        [::event/type ::create
         (fn [{product-id ::id name ::name}]
           (within (aggregate this [::assortment] product-id)
             (fail-if-exists ::id product-id)
             (publish ::created
                      ::id product-id
                      ::name name)))])))

  (stop [this]
    (apply unsubscribe* dispatcher subscriptions)
    (assoc this :subscriptions nil)))
