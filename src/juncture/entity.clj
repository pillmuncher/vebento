(ns juncture.entity
  (:require [clojure.future
             :refer :all]
            [clojure.spec
             :as s]
            [clojure.spec.test
             :as s-test]
            [juncture.util
             :refer [ns-alias]]))


(ns-alias 'ju 'juncture.core)
(ns-alias 'event 'juncture.event)


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
                   ::kind ::ju/entity
                   ::type entity-type)))


(defn- dispatch-transform
  [entity evt]
  [(::type entity) (::event/type evt)])

(s/fdef dispatch-transform
        :args (s/cat :entity (s/nilable ::ju/entity)
                     :event ::ju/event))

(s-test/instrument `dispatch-transform)

(defmulti transform dispatch-transform)
