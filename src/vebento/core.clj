(ns vebento.core
  (:require [clojure.future
             :refer :all]
            [monads.core
             :refer [mdo return fail ask asks]]
            [monads.util
             :refer [mwhen]]
            [juncture
             :refer [failure? dispatch fetch transform]]
            [componad
             :refer [within system >>=]]
            [vebento.util
             :refer [ns-alias]]))


(ns-alias 'event 'juncture.event)
(ns-alias 'entity 'juncture.entity)


(defn raise
  [event]
  (mdo
    (>>= get-dispatcher
         #(return (dispatch % event)))
    (if (failure? event)
      (fail event)
      (return event))))


(defn execute
  [& command-params]
  (>>= get-dispatcher
       #(return (dispatch-command % command-params))))

(defn execute-in
  [env & command-params]
  (within (system env)
    (execute command-params)))


(defn publish
  [& message-params]
  (>>= get-dispatcher
       #(return (dispatch-message % message-params))))

(defn publish-in
  [env & message-params]
  (within (system env)
    (publish message-params)))


(defn fail-with
  [& failure-params]
  (>>= get-dispatcher
       #(fail (dispatch-failure % failure-params))))

(defn fail-in
  [env & failure-params]
  (within (system env)
    (fail-with failure-params)))


(defn get-events
  [& {:as criteria}]
  (>>= get-journal
       #(return (apply fetch-chronological % criteria))))

(defn get-commands
  [& {:as criteria}]
  (>>= get-journal
       #(return (apply get-events % ::event/kind ::event/command criteria))))

(defn get-messages
  [& {:as criteria}]
  (>>= get-journal
       #(return (apply get-events % ::event/kind ::event/message criteria))))

(defn get-failures
  [& {:as criteria}]
  (>>= get-journal
       #(return (apply get-events % ::event/kind ::event/failure criteria))))
(defmacro def-aggregate
  [aggregate]
  `(s/def ~aggregate keyword?))

(defn aggregate
  [env aggregates entity-id]
  (fn [computation]
    (within (system env) computation)))


(defmacro m-future [& body]
  `(return (future ~@body)))


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
          (apply juncture.event/raise))))


(defn is-id-available?
  [id-key id]
  (>>= (asks :journal)
       #(-> %
            (fetch id-key id)
            (deref)
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
  (mdo
    (fail-unless-exists id-key id)
    (>>= get-journal
         #(return (future (-> %
                              (fetch-messages id-key id)
                              (deref)
                              (join)))))))


(defprotocol QueryStore
  (add-query [this query-key query-fun])
  (get-query [this query-key]))

(defmacro query
  [query-key & {:as params}]
  `(>>= ask
        #(get-query % ~query-key)
        #(m-future (% ~@params))))
