(ns vebento.testing
  (:require [clojure.future
             :refer :all]
            [clojure.test
             :refer [deftest is]]
            [clojure.spec
             :as s]
            [clojure.set
             :refer [union difference]]
            [com.stuartsierra.component
             :as co]
            [monads.core
             :refer [mdo return catch-error put-state get-state modify]]
            [monads.types
             :refer [fst snd either]]
            [monads.util
             :refer [sequence-m]]
            [juncture.event
             :as event
             :refer [fetch-apply dispatch subscribe* unsubscribe* store]]
            [componad
             :refer [run-error-rws extract >>=]]
            [vebento.core
             :refer [get-events get-dispatcher raise]]))


(defrecord MockJournal

  [trail counter]

  event/Journal

  (next-version [this]
    (swap! counter inc))

  (fetch-apply [this fun criteria]
    (future
      (->> criteria
           (reduce (fn [r [k v]] (filter #(= (k %) v) r)) @trail)
           (sort-by ::event/version)
           (fun))))

  (fetch [this criteria]
    (fetch-apply this identity criteria))

  (store [this event]
    (swap! trail conj event)
    event))


(defn mock-journal []
  (->MockJournal (atom []) (atom 0)))


(defn kind-key
  [{value ::event/kind}]
  {:key [::event/kind value]})

(defn type-key
  [{value ::event/type}]
  {:key [::event/type value]})


(defrecord MockDispatcher

  [journal handler-rel subscriptions]

  event/Dispatcher

  (subscribe
    [this [event-kind event-type handler]]
    (swap! handler-rel conj {:key [event-kind event-type] ::handler handler})
    [event-kind event-type handler])

  (unsubscribe
    [this [event-kind event-type handler]]
    (swap! handler-rel disj {:key [event-kind event-type] ::handler handler})
    [event-kind event-type handler])

  (dispatch
    [this event]
    (loop [handlers (->> #{(kind-key event) (type-key event)}
                         (clojure.set/join @handler-rel)
                         (map ::handler)
                         (set))]
      (when-not (empty? handlers)
        ((first handlers) event)
        (recur (rest handlers))))
    event)

  co/Lifecycle
  (start [this]
    (assoc this :subscriptions
           (subscribe* this
                       [::event/kind ::event/message #(store journal %)]
                       [::event/kind ::event/failure #(store journal %)])))
  (stop [this]
    (apply unsubscribe* this subscriptions)
    (assoc this :subscriptions nil)))


(defn mock-dispatcher []
  (->MockDispatcher nil (atom #{}) nil))


(defn strip-canonicals
  [events]
  (map #(dissoc % ::event/id ::event/date ::event/version) events))

(defn catch-failure
  [computation]
  (catch-error computation #(return #{%})))


(defn given
  [& m-events]
  (>>= (catch-failure (sequence-m m-events))
       put-state))

(defn after
  [& m-events]
  (>>= (catch-failure (sequence-m m-events))
       #(modify union %)))

(defn expect
  [& m-events]
  (mdo
    before <- (>>= get-state
                   #(-> % strip-canonicals set return))
    events <- (>>= (catch-failure (sequence-m m-events))
                   #(-> % strip-canonicals set return))
    after <- (>>= (get-events)
                  #(-> @% strip-canonicals set return))
    result <- (return (difference (set after) (set before)))
    (return [events result])))


(defmacro def-scenario
  [sym computation]
  `(deftest ~sym
     (let [[expected# result#] (extract (run-error-rws ~computation nil nil))]
       (is (= expected# result#)))))
