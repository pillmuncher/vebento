(ns vebento.core
  (:require [monads.core
             :as m]
            [monads.util
             :refer [mwhen map-m]]
            [juncture.event
             :as event
             :refer [command message failure failure?]]
            [juncture.entity
             :as entity
             :refer [run mutate-in]]
            [componad
             :refer [mdo-within system >>= mdo-future]]))


(def get-boundaries (m/asks :boundaries))
(def get-repository (m/asks :repository))
(def get-journal (m/asks :journal))
(def get-dispatcher (m/asks :dispatcher))


(defn boundary
  [env boundary-keys]
  (fn [computation]
    (mdo-within (system env)
      boundaries <- get-boundaries
      (run boundaries boundary-keys #(mdo-within (system env) computation)))))


(defn- -issue
  [event]
  (m/mdo
    parent <- (if (-> event ::event/parent nil?)
                (>>= m/get-state #(-> % ::event/parent m/return))
                (::event/parent event))
    (>>= get-dispatcher
         #(m/return (event/dispatch % (assoc event ::event/parent parent))))
    (if (failure? event)
      (m/fail event)
      (m/return event))))


(defn issue
  [& events]
  (map-m -issue events))


(defn call
  [command-type & command-params]
  (issue (apply command command-type command-params)))

(defn call-in
  [env command-type & command-params]
  (mdo-within (system env)
    (apply call command-type command-params)))


(defn post
  [message-type & message-params]
  (issue (apply message message-type message-params)))

(defn post-in
  [env message-type & message-params]
  (mdo-within (system env)
    (apply post message-type message-params)))


(defn fail
  [failure-type & failure-params]
  (issue (apply failure failure-type failure-params)))

(defn fail-in
  [env failure-type & failure-params]
  (mdo-within (system env)
    (apply fail failure-type failure-params)))


(defn get-events
  [& criteria]
  (>>= get-journal
       #(m/return (event/fetch % criteria))))


(def get-commands (partial get-events ::event/kind ::event/command))
(def get-messages (partial get-events ::event/kind ::event/message))
(def get-failures (partial get-events ::event/kind ::event/failure))


(defn fail-if-exists
  [id-key id]
  (>>= get-repository
       #(mwhen (entity/exists? % id-key id)
               (fail ::entity/already-exists
                      ::entity/id-key id-key
                      ::entity/id id))))

(defn fail-unless-exists
  [id-key id]
  (>>= get-repository
       #(mwhen (not (entity/exists? % id-key id))
               (fail ::entity/not-found
                      ::entity/id-key id-key
                      ::entity/id id))))


(defn get-entity
  [id-key id]
  (m/mdo
    (fail-unless-exists id-key id)
    repository <- get-repository
    (m/return (entity/fetch repository id-key id))))


(defprotocol QueryStore
  (add-query [this query-key query-fun])
  (get-query [this query-key]))


(defn query
  [query-key & params]
  (mdo-future
    (>>= m/ask #(-> % (get-query query-key) (apply params) (m/return)))))
