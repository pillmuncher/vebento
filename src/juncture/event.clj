(ns juncture.event
  (:require [clojure.future
             :refer :all]
            [clojure.spec
             :as s]
            [clojure.spec.test
             :as s-test]
            [util
             :refer [uuid inst]]))


(defprotocol Dispatcher
  (subscribe [this subscription])
  (unsubscribe [this subscription])
  (dispatch [this event]))


(defn subscribe*
  [dispatcher & subscriptions]
  (mapv #(subscribe dispatcher %) subscriptions))

(defn unsubscribe*
  [dispatcher & subscriptions]
  (mapv #(unsubscribe dispatcher %) subscriptions))

(defn dispatch*
  [dispatcher & events]
  (mapv #(dispatch dispatcher %) events))


(defprotocol Journal
  (next-version [this])
  (store [this event])
  (fetch [this criteria])
  (fetch-apply [this fun criteria]))


(defn store*
  [journal & events]
  (mapv #(store journal %) events))

(defn fetch*
  [journal & {:as criteria}]
  (fetch journal criteria))


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


(defn- -create
  [journal event-kind event-type event-params]
  (validate (assoc event-params
                   ::kind event-kind
                   ::type event-type
                   ::date (inst)
                   ::id (uuid)
                   ::version (next-version journal))))

(defn create
  [journal event-kind event-type & {:as event-params}]
  (-create journal event-kind event-type event-params))


(defn command
  [journal event-type & {:as event-params}]
  (-create journal ::command event-type event-params))

(defn message
  [journal event-type & {:as event-params}]
  (-create journal ::message event-type event-params))

(defn failure
  [journal event-type & {:as event-params}]
  (-create journal ::failure event-type event-params))


(def command? #(s/valid? (s/and valid? ::command) %))
(def message? #(s/valid? (s/and valid? ::message) %))
(def failure? #(s/valid? (s/and valid? ::failure) %))
