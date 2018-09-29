(ns vebento.core
  (:require [monads.core
             :refer [mdo return fail ask asks]]
            [monads.util
             :refer [mwhen map-m]]
            [juncture.event
             :as event
             :refer [command message failure failure?]]
            [juncture.entity
             :as entity
             :refer [run transform-in]]
            [componad
             :refer [mdo-within system >>= mdo-future]]))


(def get-boundaries (asks :boundaries))
(def get-repository (asks :repository))
(def get-journal (asks :journal))
(def get-dispatcher (asks :dispatcher))


(defn boundary
  [env boundary-keys]
  (fn [computation]
    (mdo-within (system env)
      boundaries <- get-boundaries
      (run boundaries boundary-keys #(mdo-within (system env) computation)))))


(defn issue
  [event]
  (mdo
    (>>= get-dispatcher
         #(return (event/dispatch % event)))
    (if (failure? event)
      (fail event)
      (return event))))


(defn issue*
  [& events]
  (map-m issue events))


(defn execute
  [command-type & command-params]
  (issue (apply command command-type command-params)))

(defn execute-in
  [env command-type & command-params]
  (mdo-within (system env)
    (apply execute command-type command-params)))


(defn publish
  [message-type & message-params]
  (issue (apply message message-type message-params)))

(defn publish-in
  [env message-type & message-params]
  (mdo-within (system env)
    (apply publish message-type message-params)))


(defn raise
  [failure-type & failure-params]
  (issue (apply failure failure-type failure-params)))

(defn raise-in
  [env failure-type & failure-params]
  (mdo-within (system env)
    (apply raise failure-type failure-params)))


(defn get-events
  [& criteria]
  (>>= get-journal
       #(return (event/fetch % criteria))))


(def get-commands (partial get-events ::event/kind ::event/command))
(def get-messages (partial get-events ::event/kind ::event/message))
(def get-failures (partial get-events ::event/kind ::event/failure))


(defn fail-if-exists
  [id-key id]
  (>>= get-repository
       #(mwhen (entity/exists? % id-key id)
               (raise ::entity/already-exists
                          ::entity/id-key id-key
                          ::entity/id id))))

(defn fail-unless-exists
  [id-key id]
  (>>= get-repository
       #(mwhen (not (entity/exists? % id-key id))
               (raise ::entity/not-found
                          ::entity/id-key id-key
                          ::entity/id id))))


(defn get-entity
  [id-key id]
  (mdo
    (fail-unless-exists id-key id)
    repository <- get-repository
    (return (entity/fetch repository id-key id))))


(defn update-entity [id-key event]
  (>>= (comp return :repository)
       (transform-in id-key event)))


(defprotocol QueryStore
  (add-query [this query-key query-fun])
  (get-query [this query-key]))


(defn query
  [query-key & params]
  (mdo-future
    (>>= ask #(-> % (get-query query-key) (apply params) (return)))))
