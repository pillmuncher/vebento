(ns juncture.event
  (:require [clojure.future
             :refer :all]
            [clojure.spec
             :as s]
            [clojure.spec.test
             :as s-test]))


(defprotocol Dispatcher
  (subscribe [this subscriptions])
  (unsubscribe [this subscriptions])
  (dispatch [this events]))


(defprotocol Journal
  (next-version [this])
  (store [this events])
  (fetch [this criteria])
  (fetch-apply [this fun criteria]))


(s/def ::version integer?)
(s/def ::id uuid?)
(s/def ::date inst?)
(s/def ::type keyword?)
(s/def ::kind #{::command ::message ::failure})
(s/def ::spec (s/keys :req [::kind ::type ::date ::id ::version]))

(s/def ::command #(= (::kind %) ::command))
(s/def ::message #(= (::kind %) ::message))
(s/def ::failure #(= (::kind %) ::failure))


(defmacro def-event
  [event-kind event-type {:keys [req opt]}]
  `(s/def ~event-type
     (s/and ::spec ~event-kind (s/keys :req ~req :opt ~opt))))


(defmacro def-command
  [command-type & {:as command-keys}]
  `(juncture.event/def-event ::command ~command-type ~command-keys))

(defmacro def-message
  [message-type & {:as message-keys}]
  `(juncture.event/def-event ::message ~message-type ~message-keys))

(defmacro def-failure
  [failure-type & {:as failure-keys}]
  `(juncture.event/def-event ::failure ~failure-type ~failure-keys))


(defn valid?
  [event]
  (s/valid? (::type event) event))

(defn validate
  [event]
  event)

(s/fdef validate
        :args (s/cat :event valid?))

(s-test/instrument `validate)


(defn create
  [journal event-kind event-type event-params]
  (validate (assoc event-params
                   ::kind event-kind
                   ::type event-type
                   ::date (inst)
                   ::id (uuid)
                   ::version (next-version journal))))


(defn command
  [journal event-type & {:as event-params}]
  (create journal ::command event-params))

(defn message
  [journal event-type & {:as event-params}]
  (create journal ::message event-params))

(defn failure
  [journal event-type & {:as event-params}]
  (create journal ::failure event-params))


(def command? #(s/valid? (s/and valid? ::command) %))
(def message? #(s/valid? (s/and valid? ::message) %))
(def failure? #(s/valid? (s/and valid? ::failure) %))


(defn subscribe*
  [dispatcher & subscriptions]
  (subscribe dispatcher subscriptions))

(defn unsubscribe*
  [dispatcher & subscriptions]
  (unsubscribe dispatcher subscriptions))

(defn dispatch*
  [dispatcher & events]
  (dispatch dispatcher events))

(defn dispatch-command
  [dispatcher & event-params]
  (dispatch dispatcher (apply command event-params)))

(defn dispatch-message
  [dispatcher & event-params]
  (dispatch dispatcher (apply message event-params)))

(defn dispatch-failure
  [dispatcher & event-params]
  (dispatch dispatcher (apply failure event-params)))


(defn store*
  [journal & events]
  (store journal events))

(defn fetch-chronological
  [journal & {:as criteria}]
  (fetch-apply journal #(sort-by ::version %) criteria))

(defn fetch-commands
  [journal & {:as criteria}]
  (apply fetch-chronological ::kind ::command criteria))

(defn fetch-messages
  [journal & {:as criteria}]
  (apply fetch-chronological ::kind ::message criteria))

(defn fetch-failures
  [journal & {:as criteria}]
  (apply fetch-chronological ::kind ::failure criteria))
