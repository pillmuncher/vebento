(ns juncture.event
  (:require [clojure.future
             :refer :all]
            [clojure.spec
             :as s]
            [clojure.spec.test
             :as s-test]
            [monads.core
             :refer [mdo return fail ask asks]]
            [monads.util
             :refer [sequence-m mwhen]]
            [juncture.componad
             :refer [within system >>=]]
            [juncture.util
             :refer [ns-alias inst uuid]]))


(ns-alias 'ju 'juncture.core)


(defprotocol Dispatcher
  (do-subscribe [this []])
  (do-unsubscribe [this []])
  (do-dispatch [this []]))

(defn subscribe
  [dispatcher & subscriptions]
  (do-subscribe dispatcher subscriptions))

(defn unsubscribe
  [dispatcher & subscriptions]
  (do-unsubscribe dispatcher subscriptions))

(defn dispatch
  [dispatcher & events]
  (do-dispatch dispatcher events))


(defprotocol Log
  (do-store [this event])
  (do-fetch [this criteria]))

(defn store
  [event-log & events]
  (do-store event-log events))

(defn fetch
  [event-log & {:as criteria}]
  (do-fetch event-log criteria))


(defmacro def-event
  [event-kind event-type {:keys [req opt]}]
  `(s/def ~event-type
     (s/and ::spec ~event-kind (s/keys :req ~req :opt ~opt))))


(defmacro def-command
  [command-type & {:as command-keys}]
  `(juncture.event/def-event ::ju/command ~command-type ~command-keys))

(defmacro def-message
  [message-type & {:as message-keys}]
  `(juncture.event/def-event ::ju/message ~message-type ~message-keys))

(defmacro def-failure
  [failure-type & {:as failure-keys}]
  `(juncture.event/def-event ::ju/failure ~failure-type ~failure-keys))


(defn valid?
  [event]
  (s/valid? (::type event) event))

(defn validate
  [event]
  event)

(s/fdef validate
        :args (s/cat :event valid?))

(s-test/instrument `validate)


(defn- event
  [event-kind event-type & {:as event-params}]
  (validate (assoc event-params
                   ::kind event-kind
                   ::type event-type
                   ::date (inst)
                   ::id (uuid))))


(def command (partial event ::ju/command))
(def message (partial event ::ju/message))
(def failure (partial event ::ju/failure))


(def command? #(s/valid? (s/and valid? ::ju/command) %))
(def message? #(s/valid? (s/and valid? ::ju/message) %))
(def failure? #(s/valid? (s/and valid? ::ju/failure) %))


(defn raise
  [event]
  (print "\n\n---------------" event "\n\n")
  (mdo
    (>>= (asks :dispatcher)
         #(return (dispatch % event)))
    (mwhen (failure? event)
           (fail event))
    (return event)))


(defn- -raise*
  [env events]
  (loop [events events]
    (when-not (empty? events)
      (within (system env) (raise (first events)))
      (recur (rest events)))))


(defn raise*
  [& events]
  (mdo
    (>>= ask #(return (-raise* % events)))
    (return events)))


(defn execute
  [& command-params]
  (mdo
  (raise (apply command command-params))))

(defn execute-in
  [env & command-params]
  (within (system env)
    (raise (apply command command-params))))


(defn publish
  [& message-params]
  (raise (apply message message-params)))

(defn publish-in
  [env & message-params]
  (within (system env)
    (raise (apply message message-params))))


(defn fail-with
  [& failure-params]
  (raise (apply failure failure-params)))

(defn fail-in
  [env & failure-params]
  (within (system env)
    (raise (apply failure failure-params))))


(defn get-events
  [& {:as criteria}]
  (>>= (asks :event-log)
       #(-> % (do-fetch criteria) (return))))


(def get-commands (partial get-events ::kind ::ju/command))
(def get-messages (partial get-events ::kind ::ju/message))
(def get-failures (partial get-events ::kind ::ju/failure))
