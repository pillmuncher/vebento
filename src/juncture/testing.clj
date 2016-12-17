(ns juncture.testing
  (:require [clojure.future
             :refer :all]
            [clojure.test
             :refer [deftest is]]
            [clojure.spec
             :as s]
            [monads.core
             :as monad
             :refer [mdo return catch-error put-state get-state]]
            [monads.types
             :refer [fst snd]]
            [juncture.componad
             :refer [>>=]]
            [juncture.event
             :as event
             :refer [get-events raise raise*]]))


(defrecord TestingEventLog

  [trail]

  event/Log

  (do-store [this events]
    (swap! trail clojure.set/union events))

  (do-fetch [this criteria]
    (->> @trail
         (clojure.set/join #{criteria})
         (sort-by :event/date))))

(defn testing-event-log []
  (->TestingEventLog (atom #{})))


(defn kind-key
  [{value ::event/kind}]
  {:key [::event/kind value]})

(defn type-key
  [{value ::event/type}]
  {:key [::event/type value]})

(defrecord TestingEventDispatcher

  [event-log handler-rel]

  event/Dispatcher

  (do-subscribe
    [this subscriptions]
    (loop [subscriptions subscriptions]
      (when-not (empty? subscriptions)
        (let [[k v h] (first subscriptions)]
          (swap! handler-rel conj {:key [k v] ::handler h})
          (recur (rest subscriptions)))))
    subscriptions)

  (do-unsubscribe
    [this subscriptions]
    (when-not (empty? subscriptions)
      (let [[k v h] (first subscriptions)]
        (swap! handler-rel disj {:key [k v] ::handler h})
        (recur (rest subscriptions)))))

  (do-dispatch
    [this events]
    (when-not (empty? events)
      (let [event (first events)]
        (loop [handlers (->> #{(kind-key event) (type-key event)}
                             (clojure.set/join @handler-rel)
                             (map ::handler)
                             (set))]
          (when-not (empty? handlers)
            ((first handlers) event)
            (recur (rest handlers)))))
      (recur (rest events)))))

(defn testing-event-dispatcher []
  (->TestingEventDispatcher nil (atom #{})))


(def given raise*)

(defn after
  [event]
  (mdo
    (>>= (get-events)
         #(-> % (conj event) (put-state)))
    (catch-error
      (raise event)
      return)))

(defn expect
  [& events]
  (mdo
    raised <- (get-events)
    result <- (>>= get-state
                   #(return (clojure.set/difference (set raised) %)))
    (return [(->> result (map #(dissoc % ::event/id ::event/date)) (set))
             (->> events (map #(dissoc % ::event/id ::event/date)) (set))])))


(defmacro def-scenario
  [sym computation]
  `(deftest ~sym
     (let [[events# result#] (snd (fst (mdo ~computation)))]
       (is (= events# result#)))))
