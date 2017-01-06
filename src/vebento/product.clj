(ns vebento.product
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
             :refer [boundary publish execute fail-with fail-if-exists
                     fail-unless-exists transform-in get-entity]]
            [vebento.specs
             :as specs]))


(ns-alias 'customer 'vebento.customer)
(ns-alias 'merchant 'vebento.merchant)
(ns-alias 'order 'vebento.order)


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

  [boundaries dispatcher journal entity-store subscriptions]

  co/Lifecycle

  (start [this]

    (register boundaries [::assortment])

    (assoc
      this :subscriptions
      (subscribe*
        dispatcher

        [::created
         (transform-in entity-store ::id)]

        [::create
         (fn [{product-id ::id name ::name}]
           (within (boundary this #{::assortment})
             (fail-if-exists ::id product-id)
             (publish ::created
                      ::id product-id
                      ::name name)))])))

  (stop [this]
    (apply unsubscribe* dispatcher subscriptions)
    (assoc this :subscriptions nil)))
