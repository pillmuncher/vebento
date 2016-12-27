(ns juncture.entity
  (:require [clojure.future
             :refer :all]
            [clojure.spec
             :as s]
            [clojure.spec.test
             :as s-test]
            [clojure.set
             :refer [union difference]]
            [juncture.event
             :as event
             :refer [def-failure fetch-apply]]))


(def-failure ::already-exists
  :req [::id-key
        ::id])

(def-failure ::not-found
  :req [::id-key
        ::id])


(s/def ::id-key keyword?)
(s/def ::version integer?)
(s/def ::id uuid?)
(s/def ::type keyword?)
(s/def ::kind #{::entity})
(s/def ::spec (s/keys :req [::kind ::type ::id ::version]))

(s/def ::entity #(= (::kind %) ::entity))


(defprotocol Aggregate
  (register [this aggs])
  (unregister [this aggs])
  (run [this aggs fun]))

(defn aggregates
  [delegate]
  (let [a (atom #{})]
    (reify Aggregate
      (register [this aggs]
        (swap! a union aggs))
      (unregister [this aggs]
        (swap! a #(difference aggs %)))
      (run [this aggs fun]
        (delegate @a aggs fun)))))


(defmacro def-entity
  [entity-type & {:keys [req opt]}]
  `(s/def ~entity-type
     (s/and ::spec (s/keys :req ~req :opt ~opt))))


(defn valid?
  [entity]
  (s/valid? (::type entity) entity))

(defn- validate
  [entity]
  entity)

(s/fdef validate
        :args (s/cat :entity valid?))

(s-test/instrument `validate)


(defn create
  [entity-type & {:as entity-params}]
  (validate (assoc entity-params
                   ::kind ::entity
                   ::type entity-type
                   ::version 0)))


(defn- transformer
  [entity evt]
  [(::type entity) (::event/type evt)])

(s/fdef transformer
        :args (s/cat :entity (s/nilable ::entity)
                     :event ::event/spec))

(s-test/instrument `transformer)

(defmulti transform transformer)

(defmethod transform
  :default
  [entity _]
  entity)


(defn attach-event-id
  [fun]
  (fn [entity event]
    (-> entity
        (fun event)
        (assoc ::event/id (::event/id event)))))

(defn projection
  ([fun]
   (projection fun nil))
  ([fun start]
   (fn [events]
     (reduce (attach-event-id fun) start events))))


(defprotocol EntityStore
  (store-entity [this entity])
  (fetch-entity [this id])
  (exists-entity? [this id]))


(defn upgrade-entity
  [entity-store id-key]
  (fn [event]
    (let [entity (fetch-entity entity-store (id-key event))]
      (store-entity entity-store (transform @entity event)))))
