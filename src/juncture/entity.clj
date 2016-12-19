(ns juncture.entity
  (:require [clojure.future
             :refer :all]
            [clojure.spec
             :as s]
            [clojure.spec.test
             :as s-test]
            [juncture.event
             :as event
             :refer [def-failure fetch-apply]]))


(s/def ::version integer?)
(s/def ::id uuid?)
(s/def ::type keyword?)
(s/def ::kind #{::entity})
(s/def ::spec (s/keys :req [::kind ::type ::id ::version]))

(s/def ::entity #(= (::kind %) ::entity))


(s/def ::id-key keyword?)

(def-failure ::already-exists
  :req [::id-key
        ::id])

(def-failure ::not-found
  :req [::id-key
        ::id])


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


(defn run-transformer
  [entity event]
  (update (transform entity event) assoc ::id (::event/id event)))


(defn join
  ([events]
   (join nil events))
  ([start events]
   (reduce run-transformer start events)))

(defn fetch-entity
  [journal id-key id]
  (fetch-apply journal join {::event/kind ::event/message
                             id-key id}))
