(ns vebento.core
  (:require [clojure.future
             :refer :all]
            [monads.core
             :refer [mdo return fail ask asks]]
            [monads.util
             :refer [mwhen sequence-m]]
            [juncture.event
             :as event
             :refer [command message failure failure? dispatch fetch fetch*]]
            [juncture.entity
             :as entity
             :refer [run-transformer fetch-entity]]
            [componad
             :refer [within system >>=]]))


(def get-journal (asks :journal))
(def get-dispatcher (asks :dispatcher))


(defn raise
  [event]
  (mdo
    (>>= get-dispatcher
         #(return (dispatch % event)))
    (mwhen (failure? event)
           (fail event))
    (return event)))

(defn raise*
  [& events]
  (sequence-m (for [event events] (raise event))))


(defn execute
  [command-type & command-params]
  (>>= get-journal
       #(raise (apply command % command-type command-params))))

(defn execute-in
  [env command-type & command-params]
  (within (system env)
    (apply execute command-type command-params)))


(defn publish
  [message-type & message-params]
  (>>= get-journal
       #(raise (apply message % message-type message-params))))

(defn publish-in
  [env message-type & message-params]
  (within (system env)
    (apply publish message-type message-params)))


(defn fail-with
  [failure-type & failure-params]
  (>>= get-journal
       #(raise (apply failure % failure-type failure-params))))

(defn fail-in
  [env failure-type & failure-params]
  (within (system env)
    (apply fail-with failure-type failure-params)))


(defn get-events
  [& {:as criteria}]
  (>>= get-journal
       #(return (fetch % criteria))))

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


;;FIXME: call to raise is bogus, also it is not clear as yet how to multi-fail.
(defmacro f-mwhen
  [& {:as expression-failure-pairs}]
  (let [possible-failures (->> expression-failure-pairs
                               (map (fn [pair] `(future (when ~@pair))))
                               (vec))]
    `(->> ~possible-failures
          (keep deref)
          (apply raise*))))


(defn is-id-available?
  [id-key id]
  (>>= get-journal
       #(-> (fetch* % ::event/kind ::event/message id-key id)
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


(defn get-entity
  [id-key id]
  (mdo
    (fail-unless-exists id-key id)
    journal <- get-journal
    (return (fetch-entity journal id-key id))))


(defprotocol QueryStore
  (add-query [this query-key query-fun])
  (get-query [this query-key]))

(defmacro query
  [query-key & {:as params}]
  `(>>= ask
        #(get-query % ~query-key)
        #(m-future (% ~@params))))
