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
             :refer [fetch-apply dispatch subscribe* unsubscribe* store store*]]
            [componad
             :refer [run-error-rws extract >>=]]
            [vebento.core
             :refer [get-events get-dispatcher raise*]]))


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
                       [::event/kind ::event/message #(store* journal %)]
                       [::event/kind ::event/failure #(store* journal %)])))
  (stop [this]
    (apply unsubscribe* this subscriptions)
    (assoc this :subscriptions nil)))


(defn mock-dispatcher []
  (->MockDispatcher nil (atom #{}) nil))


(defn strip-canonicals
  [events]
  (->> events
       (map #(dissoc % ::event/id ::event/date ::event/version))
       (set)
       (return)))


(defn keep-events
  [what m-events]
  (mdo
    (>>= (sequence-m m-events)
         #(catch-error (apply raise* %) return))
    state <- get-state
    (>>= (get-events)
         #(strip-canonicals @%)
         #(modify assoc what %))))

(defn given
  [& m-events]
  (keep-events ::given m-events))

(defn after
  [& m-events]
  (keep-events ::after m-events))

(defn expect
  [& m-events]
  (mdo
    state <- get-state
    given <- (return (::given state))
    after <- (return (::after state))
    events <- (>>= (sequence-m m-events) strip-canonicals)
    (return [events (difference after given)])))


(defmacro def-scenario
  [sym computation]
  `(deftest ~sym
     (let [[expected# result#] (extract (run-error-rws ~computation nil nil))]
       (is (= expected# result#)))))
