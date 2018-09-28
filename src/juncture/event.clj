(ns juncture.event
  (:require [clojure.spec.alpha
             :as s]
            [clojure.spec.test.alpha
             :as s-test]
            [util
             :refer [uuid inst]]))


(defprotocol Dispatcher
  (subscribe [this subscription])
  (unsubscribe [this subscription])
  (dispatch [this event]))


(defn subscribe-maps
  [dispatcher & subscription-maps]
  (->> subscription-maps
       (apply merge-with concat)
       (mapv #(subscribe dispatcher %))))

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
  (store [this event])
  (fetch [this criteria])
  (fetch-apply [this fun criteria]))


(defn store-in
  [journal]
  (fn [event]
    (store journal event)))

(defn store*
  [journal & events]
  (mapv (store-in journal) events))

(defn fetch*
  [journal & {:as criteria}]
  (fetch journal criteria))


(s/def ::version integer?)
(s/def ::id uuid?)
(s/def ::date inst?)
(s/def ::type keyword?)
(s/def ::kind #{::command ::notice ::error})
(s/def ::spec (s/keys :req [::kind ::type ::date ::id]
                      :opt [::version]))

(s/def ::command #(= (::kind %) ::command))
(s/def ::notice #(= (::kind %) ::notice))
(s/def ::error #(= (::kind %) ::error))


(defmulti handle ::type)


(defmacro def-event
  [event-kind event-type {:keys [req opt]}]
  `(s/def ~event-type
     (s/and ::spec ~event-kind (s/keys :req ~req :opt ~opt))))


(defmacro def-command
  [command-type & {:as command-keys}]
  `(def-event ::command ~command-type ~command-keys))

(defmacro def-notice
  [notice-type & {:as notice-keys}]
  `(def-event ::notice ~notice-type ~notice-keys))

(defmacro def-error
  [error-type & {:as error-keys}]
  `(def-event ::error ~error-type ~error-keys))


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
  [event-kind event-type event-params]
  (validate (assoc event-params
                   ::kind event-kind
                   ::type event-type
                   ::date (inst)
                   ::id (uuid))))

(defn create
  [event-kind event-type & {:as event-params}]
  (-create event-kind event-type event-params))


(defn command
  [event-type & {:as event-params}]
  (-create ::command event-type event-params))

(defn notice
  [event-type & {:as event-params}]
  (-create ::notice event-type event-params))

(defn error
  [event-type & {:as event-params}]
  (-create ::error event-type event-params))


(def command? #(s/valid? (s/and valid? ::command) %))
(def notice? #(s/valid? (s/and valid? ::notice) %))
(def error? #(s/valid? (s/and valid? ::error) %))
