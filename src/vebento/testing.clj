(ns vebento.testing
  (:require [clojure.future
             :refer :all]
            [clojure.test
             :refer [deftest is]]
            [clojure.spec
             :as s]
            [clojure.set
             :refer [difference]]
            [com.stuartsierra.component
             :as co]
            [monads.core
             :refer [mdo return catch-error get-state modify]]
            [monads.util
             :refer [sequence-m]]
            [juncture.event
             :as event
             :refer [fetch-apply dispatch subscribe* unsubscribe* store store*]]
            [componad
             :refer [run-error-rws extract >>=]]
            [vebento.core
             :refer [get-events raise*]]))


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


(defn filter-set
  [{kind ::event/kind type ::event/type}]
  #{{:key [::event/kind kind]}
    {:key [::event/type type]}})


(defrecord MockDispatcher

  [journal handler-rel subscriptions]

  event/Dispatcher

  (subscribe
    [this [event-key event-val handler]]
    (swap! handler-rel conj {:key [event-key event-val] ::handler handler})
    [event-key event-val handler])

  (unsubscribe
    [this [event-key event-val handler]]
    (swap! handler-rel disj {:key [event-key event-val] ::handler handler})
    [event-key event-val handler])

  (dispatch
    [this event]
    (mapv #(% event) (->> (filter-set event)
                          (clojure.set/join @handler-rel)
                          (map ::handler)
                          (set)))
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
       (return)))


(defn raise-and-keep
  [what m-events]
  (mdo
    (>>= (sequence-m m-events)
         #(catch-error (apply raise* %) return))
    (>>= (get-events)
         #(strip-canonicals @%)
         #(modify assoc what %))))

(defn given
  [& m-events]
  (raise-and-keep ::given m-events))

(defn after
  [& m-events]
  (raise-and-keep ::after m-events))

(defn expect
  [& m-events]
  (mdo
    state <- get-state
    given <- (return (::given state))
    after <- (return (::after state))
    events <- (>>= (sequence-m m-events) strip-canonicals)
    (return [(set events) (difference (set after) (set given))])))


(defmacro def-scenario
  [sym computation]
  `(deftest ~sym
     (let [[expected# result#] (extract (run-error-rws ~computation nil nil))]
       (is (= expected# result#)))))
