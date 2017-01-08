(ns vebento.core
  (:require [clojure.future
             :refer :all]
            [monads.core
             :refer [mdo return fail ask asks]]
            [monads.util
             :refer [mwhen]]
            [juncture.event
             :as event
             :refer [command message failure failure?]]
            [juncture.entity
             :as entity
             :refer [run transform]]
            [componad
             :refer [within system >>= mdo-future]]))


(def get-componad (asks :componad))


(defn boundary
  [env boundary-keys]
  (fn [computation]
    (within (system env)
      componad <- get-componad
      (run componad boundary-keys #(within (system env) computation)))))


(defn transform-in
  [componad id-key]
  (fn [event]
    (let [entity (entity/fetch componad id-key (id-key event))]
      (entity/store componad id-key (transform @entity event)))))


(defn raise
  [event]
  (mdo
    (>>= get-componad
         #(return (event/dispatch % event)))
    (if (failure? event)
      (fail event)
      (return event))))


(defn execute
  [command-type & command-params]
  (raise (apply command command-type command-params)))

(defn execute-in
  [env command-type & command-params]
  (within (system env)
    (apply execute command-type command-params)))


(defn publish
  [message-type & message-params]
  (raise (apply message message-type message-params)))

(defn publish-in
  [env message-type & message-params]
  (within (system env)
    (apply publish message-type message-params)))


(defn fail-with
  [failure-type & failure-params]
  (raise (apply failure failure-type failure-params)))

(defn fail-in
  [env failure-type & failure-params]
  (within (system env)
    (apply fail-with failure-type failure-params)))


(defn get-events
  [& {:as criteria}]
  (>>= get-componad
       #(return (event/fetch % criteria))))

(defn get-commands
  [& {:as criteria}]
  (>>= get-componad
       #(return (apply get-events % ::event/kind ::event/command criteria))))

(defn get-messages
  [& {:as criteria}]
  (>>= get-componad
       #(return (apply get-events % ::event/kind ::event/message criteria))))

(defn get-failures
  [& {:as criteria}]
  (>>= get-componad
       #(return (apply get-events % ::event/kind ::event/failure criteria))))


(defn fail-if-exists
  [id-key id]
  (>>= get-componad
       #(mwhen (entity/exists? % id-key id)
               (fail-with ::entity/already-exists
                          ::entity/id-key id-key
                          ::entity/id id))))

(defn fail-unless-exists
  [id-key id]
  (>>= get-componad
       #(mwhen (not (entity/exists? % id-key id))
               (fail-with ::entity/not-found
                          ::entity/id-key id-key
                          ::entity/id id))))


(defn get-entity
  [id-key id]
  (mdo
    (fail-unless-exists id-key id)
    componad <- get-componad
    (return (entity/fetch componad id-key id))))


(defprotocol QueryStore
  (add-query [this query-key query-fun])
  (get-query [this query-key]))


(defn query
  [query-key & params]
  (mdo-future
    (>>= ask #(-> % (get-query query-key) (apply params) (return)))))
