(ns vebento.testing
  (:require [clojure.future
             :refer :all]
            [clojure.test
             :refer [deftest is]]
            [clojure.spec
             :as s]
            [clojure.spec.test
             :as stest]
            [clojure.spec.gen
             :as gen]
            [clojure.set
             :refer [union difference]]
            [com.stuartsierra.component
             :as co]
            [monads.core
             :refer [mdo return catch-error modify]]
            [monads.util
             :refer [sequence-m]]
            [util
             :refer [zip]]
            [componad
             :refer [run-componad within system extract return* >>=]]
            [juncture.event
             :as event
             :refer [fetch-apply dispatch subscribe* unsubscribe* store store*]]
            [vebento.core
             :refer [get-events raise*]]))


(defrecord MockJournal

  [trail]

  event/Journal

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
  (->MockJournal (atom [])))


(defrecord MockDispatcher

  [journal counter subscriptions]

  event/Dispatcher

  (subscribe
    [this [event-key event-val handler]]
    (swap! subscriptions update [event-key event-val] conj handler)
    [event-key event-val handler])

  (unsubscribe
    [this _])

  (dispatch
    [this event]
    (let [event (assoc event ::event/version (swap! counter inc))]
      (->> (select-keys event [::event/kind ::event/type])
           (map @subscriptions)
           (apply union)
           (mapv #(% event)))
      event))

  co/Lifecycle
  (start [this]
    (subscribe* this
                [::event/kind ::event/message #(store* journal %)]
                [::event/kind ::event/failure #(store* journal %)])
    this)
  (stop [this]
    (assoc this subscriptions nil)))


(defn mock-dispatcher []
  (->MockDispatcher nil (atom 0) (atom {})))


(defn- strip-canonicals
  [events]
  (->> events
       (map #(dissoc % ::event/id ::event/date ::event/version))
       (return)))


(defn- raise-events
  [events]
  (mdo
    (>>= (return* events)
         #(apply raise* %))
    (>>= (get-events)
         #(strip-canonicals @%))))


(defn run-scenario
  [& {:keys [using given after await]}]
  (extract
    (run-componad
      (mdo
        given-events <- (raise-events given)
        after-events <- (raise-events after)
        await-events <- (>>= (return* await) strip-canonicals)
        (return [(set await-events) (difference (set after-events)
                                                (set given-events))]))
      :component using)))


(defn param-specs
  [params]
  (for [[p-spec p] params]
    [`~p `[~(keyword p) ~p-spec]]))


(defmacro def-scenario
  [sym params & body]
  (let [[fn-params fspec-params] (->> params (partition 2) (param-specs) (zip))]
    `(deftest ~sym
       (let [test-fn-spec# (s/fspec :args (s/cat ~@(flatten fspec-params)))
             test-fn# (fn [~@fn-params] (run-scenario ~@body))]
         (mapv
           (fn [[expected# result#]] (is (= expected# result#)))
           (map second (s/exercise-fn test-fn# 10 test-fn-spec#)))))))
