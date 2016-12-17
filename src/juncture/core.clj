(ns juncture.core
  (:require [clojure.future
             :refer :all]
            [monads.core
             :as monad
             :refer [return ask asks]]
            [monads.util
             :refer [mwhen]]
            [juncture.componad
             :refer [within system component >>=]]
            [juncture.util
             :refer [ns-alias]]
            [juncture.event
             :as event
             :refer [def-event def-command def-message def-failure
                     command message failure command? message? failure?
                     execute publish fail-with execute-in publish-in fail-in
                     get-events get-commands get-messages get-failures
                     subscribe unsubscribe dispatch store fetch raise raise*]]
            [juncture.entity
             :as entity
             :refer [def-entity create transform]]
            [juncture.specs]))


(defmacro def-aggregate
  [aggregate]
  `(s/def ~aggregate keyword?))

(defn aggregate
  [env aggregates entity-id]
  (fn [computation]
    (within (system env) computation)))


(defmacro m-future [f]
  `(return (future ~f)))

(defmacro mdo-future
  [& computations]
  `(>>= ask #(m-future (within (system %) ~@computations))))


(defmacro m-when
  [m-condition computation]
  `(>>= ~m-condition #(mwhen % ~computation)))

(defmacro m-unless
  [m-condition computation]
  `(>>= ~m-condition #(mwhen (not %) ~computation)))


(defmacro f-mwhen
  [& {:as expression-failure-pairs}]
  (let [possible-failures (->> expression-failure-pairs
                               (map (fn [pair] `(future (when ~@pair))))
                               (vec))]
    `(->> ~possible-failures
          (keep deref)
          (apply juncture.event/raise*))))


(defn is-id-available?
  [id-key id]
  (>>= (asks :event-log)
       #(-> %
            (fetch id-key id)
            (empty?)
            (return))))

(defn fail-if-exists
  [id-key id]
  (m-unless (is-id-available? id-key id)
            (fail-with ::entity/already-exists
                       ::entity/id-key id-key
                       ::entity/id id)))

(defn fail-unless-exists
  [id-key id]
  (m-when (is-id-available? id-key id)
          (fail-with ::entity/not-found
                     ::entity/id-key id-key
                     ::entity/id id)))


(defn join
  ([events]
   (join nil events))
  ([start events]
   (return (reduce transform start events))))

(defn get-entity
  [id-key id]
  (mdo-future
    (fail-unless-exists id-key id)
    (>>= (get-messages id-key id)
         join)))


(defprotocol QueryStore
  (add-query [this query-key query-fun])
  (get-query [this query-key]))

(defmacro query
  [query-key & {:as params}]
  `(>>= ask
        #(get-query % ~query-key)
        #(m-future (% ~@params))))
