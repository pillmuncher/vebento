(ns juncture
  (:require [clojure.future
             :refer :all]
            [clojure.spec
             :as s]
            [clojure.spec.test
             :as s-test]
            [clojure.set
             :refer [union difference]]
            [util
             :refer [uuid inst]]
            [juncture.event
             :as event]
            [juncture.entity
             :as entity]))


;(defmacro def-command
  ;[command-type & {:as command-keys}]
  ;`(event/def ::event/command ~command-type ~command-keys))

;(defmacro def-message
  ;[message-type & {:as message-keys}]
  ;`(event/def ::event/message ~message-type ~message-keys))

;(defmacro def-failure
  ;[failure-type & {:as failure-keys}]
  ;`(event/def ::event/failure ~failure-type ~failure-keys))


;(defprotocol boundary
  ;(register [this aggs])
  ;(unregister [this aggs])
  ;(run [this aggs fun]))


;(defn boundaries
  ;[delegate]
  ;(let [a (atom #{})]
    ;(reify boundary
      ;(register [this aggs]
        ;(swap! a union aggs))
      ;(unregister [this aggs]
        ;(swap! a #(difference aggs %)))
      ;(run [this aggs fun]
        ;(delegate @a aggs fun)))))


;(defprotocol Dispatcher
  ;(subscribe [this subscription])
  ;(unsubscribe [this subscription])
  ;(dispatch [this event]))


;(defn subscribe*
  ;[dispatcher & subscriptions]
  ;(mapv #(subscribe dispatcher %) subscriptions))

;(defn unsubscribe*
  ;[dispatcher & subscriptions]
  ;(mapv #(unsubscribe dispatcher %) subscriptions))

;(defn dispatch*
  ;[dispatcher & events]
  ;(mapv #(dispatch dispatcher %) events))


;(defprotocol Journal
  ;(event/store [this event])
  ;(event/fetch [this criteria])
  ;(event/fetch-apply [this fun criteria]))


;(defn store*
  ;[journal & events]
  ;(mapv #(event/store journal %) events))

;(defn fetch*
  ;[journal & {:as criteria}]
  ;(event/fetch journal criteria))


;(s/def ::version integer?)
;(s/def ::id-key keyword?)
;(s/def ::id uuid?)
;(s/def ::date inst?)
;(s/def ::type keyword?)
;(s/def ::kind #{::command ::message ::failure})
;(s/def ::spec (s/keys :req [::kind ::type ::date ::id]
                      ;:opt [::version]))

;(s/def ::entity #(= (::kind %) ::entity))
;(s/def ::command #(= (::kind %) ::command))
;(s/def ::message #(= (::kind %) ::message))
;(s/def ::failure #(= (::kind %) ::failure))


;(defmacro def-entity
  ;[entity-type & {:keys [req opt]}]
  ;`(s/def ~entity-type
     ;(s/and ::spec (s/keys :req ~req :opt ~opt))))


;(defmacro def-event
  ;[event-kind event-type {:keys [req opt]}]
  ;`(s/def ~event-type
     ;(s/and ::spec ~event-kind (s/keys :req ~req :opt ~opt))))


;(defmacro def-command
  ;[command-type & {:as command-keys}]
  ;`(def-event ::command ~command-type ~command-keys))

;(defmacro def-message
  ;[message-type & {:as message-keys}]
  ;`(def-event ::message ~message-type ~message-keys))

;(defmacro def-failure
  ;[failure-type & {:as failure-keys}]
  ;`(def-event ::failure ~failure-type ~failure-keys))


;(defn valid? [e] (s/valid? (::type e) e))
;(defn validate [e] e)
;(s/fdef validate :args (s/cat :e valid?))
;(s-test/instrument `validate)


;(defn- -create
  ;[kind type params]
  ;(-> {::kind kind
       ;::type type
       ;::date (inst)
       ;::id (uuid)}
      ;(merge params)
      ;(validate)))

;(defn create
  ;[kind type & {:as params}]
  ;(-create kind type params))


;(defn event
  ;[event-type & {:as event-params}]
  ;(-create ::entity event-type event-params))

;(defn command
  ;[entity-type & {:as entity-params}]
  ;(-create ::command entity-type entity-params))

;(defn message
  ;[entity-type & {:as entity-params}]
  ;(-create ::message entity-type entity-params))

;(defn failure
  ;[entity-type & {:as entity-params}]
  ;(-create ::failure entity-type entity-params))


;(def entity? #(s/valid? (s/and valid? ::entity) %))
;(def command? #(s/valid? (s/and valid? ::command) %))
;(def message? #(s/valid? (s/and valid? ::message) %))
;(def failure? #(s/valid? (s/and valid? ::failure) %))


;(defn- transformer
  ;[entity evt]
  ;[(::type entity) (::type evt)])

;(s/fdef transformer
        ;:args (s/cat :entity (s/nilable ::entity)
                     ;:event ::spec))

;(s-test/instrument `transformer)

;(defmulti transform transformer)

;(defmethod transform
  ;:default
 ;[entity _]
 ;entity)

;(defn run-transformer
  ;[entity event]
  ;(assoc (transform entity event) ::id (::id event)))


;(defn projection
  ;([events]
   ;(projection nil events))
  ;([start events]
   ;(reduce run-transformer start events)))

;(defn entity/fetch
  ;[journal id-key id]
  ;(event/fetch-apply journal projection {::kind ::message


;(def-failure ::entity/already-exists
  ;:req [::id-key
        ;::id])

;(def-failure ::entity/not-found
  ;:req [::id-key
;::id])
