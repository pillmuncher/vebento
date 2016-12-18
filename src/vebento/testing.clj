(ns vebento.testing
  (:require [clojure.future
             :refer :all]
            [clojure.test
             :refer [deftest is]]
            [monads.core
             :refer [mdo return catch-error put-state get-state]]
            [monads.types
             :refer [fst snd either]]
            [componad
             :refer [>>= extract]]
            [juncture
             :as event
             :refer [fetch-apply get-events raise raise*]]))


(defrecord TestingEventJournal

  [trail]

  event/Journal

  (fetch-apply [this fun criteria]
    (future (->> @trail
                 (clojure.set/join #{criteria})
                 (fun))))

  (fetch [this criteria]
    (fetch-apply this identity criteria))

  (store [this events]
    (swap! trail clojure.set/union events)))


(defn testing-journal []
  (->TestingEventJournal (atom #{})))


(defn kind-key
  [{value ::event/kind}]
  {:key [::event/kind value]})

(defn type-key
  [{value ::event/type}]
  {:key [::event/type value]})

(defrecord TestingEventDispatcher

  [journal handler-rel]

  event/Dispatcher

  (subscribe
    [this subscriptions]
    (loop [subscriptions subscriptions]
      (when-not (empty? subscriptions)
        (let [[k v h] (first subscriptions)]
          (swap! handler-rel conj {:key [k v] ::handler h})
          (recur (rest subscriptions)))))
    subscriptions)

  (unsubscribe
    [this subscriptions]
    (when-not (empty? subscriptions)
      (let [[k v h] (first subscriptions)]
        (swap! handler-rel disj {:key [k v] ::handler h})
        (recur (rest subscriptions)))))

  (dispatch
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
         #(-> @% (conj event) (put-state)))
    (catch-error
      (raise event)
      return)))

(defn expect
  [& events]
  (mdo
    raised <- (get-events)
    result <- (>>= get-state
                   #(->> % (clojure.set/difference (set @raised)) (return)))
    (return [(->> result
                  (map #(dissoc % ::event/id ::event/date ::event/version))
                  (set))
             (->> events
                  (map #(dissoc % ::event/id ::event/date ::event/version))
                  (set))])))


(defmacro def-scenario
  [sym computation]
  `(deftest ~sym
     (let [[events# result#] ~computation]
       (is (= events# result#)))))
