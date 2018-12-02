(ns vebento.product
  (:require [clojure.spec.alpha
             :as s]
            [com.stuartsierra.component
             :as co]
            [util
             :refer [ns-alias]]
            [juncture.event
             :as event
             :refer [defcommand defmessage deffailure
                     subscribe-maps unsubscribe*]]
            [juncture.entity
             :as entity
             :refer [register unregister defentity create mutate
                     mutate-in]]
            [componad
             :refer [mdo-within]]
            [vebento.core
             :refer [boundary post call fail fail-if-exists
                     fail-unless-exists get-entity]]
            [vebento.specs
             :as specs]))


(ns-alias 'customer 'vebento.customer)
(ns-alias 'merchant 'vebento.merchant)
(ns-alias 'order 'vebento.order)


(s/def ::id ::specs/id)
(s/def ::name ::specs/name)
(s/def ::amount ::specs/amount)


(defcommand ::create
  :req [::id
        ::name])


(defmessage ::created
  :req [::id
        ::name])


(defentity ::entity
  :req [::name])


(defmethod mutate
  [nil ::created]
  [_ {product-id ::id
      name ::name}]
  (create ::entity
          ::entity/id product-id
          ::name name))


(defrecord Component
  [boundaries repository journal dispatcher subscriptions]
  co/Lifecycle
  (start [this]
    (register boundaries [::assortment])
    (assoc
      this :subscriptions
      (subscribe-maps dispatcher
                      {::created [(mutate-in repository ::id)]
                       ::create [(fn [{product-id ::id name ::name}]
                                   (mdo-within (boundary this #{::assortment})
                                     (fail-if-exists ::id product-id)
                                     (post ::created
                                              ::id product-id
                                              ::name name)))]})))
  (stop [this]
    (apply unsubscribe* dispatcher subscriptions)
    (unregister boundaries [::assortment])
    (assoc this :subscriptions nil)))
