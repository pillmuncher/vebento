(ns juncture.entity
  (:require [clojure.spec.alpha
             :as s]
            [clojure.spec.test.alpha
             :as s-test]
            [juncture.event
             :as event
             :refer [deffailure]]))


(deffailure ::already-exists
  :req [::id-key
        ::id])

(deffailure ::not-found
  :req [::id-key
        ::id])


(s/def ::id-key keyword?)
(s/def ::version integer?)
(s/def ::id uuid?)
(s/def ::type keyword?)
(s/def ::kind #{::entity})
(s/def ::spec (s/keys :req [::kind ::type ::id ::version]))

(s/def ::entity #(= (::kind %) ::entity))


(defmacro defentity
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


(defn- mutate-dispatcher
  [entity evt]
  [(::type entity) (::event/type evt)])

(s/fdef mutate-dispatcher
        :args (s/cat :entity (s/nilable ::entity)
                     :event ::event/spec))

(s-test/instrument `mutate-dispatcher)

(defmulti mutate mutate-dispatcher)

(defmethod mutate
  :default
  [entity _]
  entity)


(defprotocol Boundaries
  (register [this aggs])
  (unregister [this aggs])
  (run [this aggs fun]))


(defprotocol Repository
  (store [this id-key entity])
  (fetch [this id-key id])
  (exists? [this id-key id]))


(defn mutate-in ;; TODO: make atomic.
  [repository id-key]
  (fn [event]
    (let [entity (fetch repository id-key (id-key event))]
      (store repository id-key (mutate @entity event)))))


(defn- run-and-attach-event-id
  [fun]
  (fn [entity event]
    (assoc (fun entity event) ::event/id (::event/id event))))

(defn projection
  ([fun]
   (projection fun nil))
  ([fun start]
   (fn [events]
     (reduce (run-and-attach-event-id fun) start events))))
