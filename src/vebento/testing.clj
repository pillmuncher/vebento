(ns vebento.testing
  (:require [clojure.test
             :refer [deftest is]]
            [clojure.spec.alpha
             :as s]
            [clojure.spec.test.alpha
             :as stest]
            [clojure.spec.gen.alpha
             :as gen]
            [clojure.set
             :refer [union difference]]
            [com.stuartsierra.component
             :as co]
            [monads.core
             :refer [mdo return]]
            [monads.util
             :refer [map-m]]
            [util
             :refer [zip]]
            [componad
             :refer [mdo-within return* >>=]]
            [juncture.event
             :as event
             :refer [subscribe-maps store-in unsubscribe*]]
            [juncture.entity
             :as entity]
            [vebento.core
             :refer [relay get-events]]))


(defrecord MockBoundaries
  []
  entity/Boundaries

  (register [this boundary-keys])

  (unregister [this boundary-keys])

  (run [this boundary-keys fun]
    (fun)))


(defrecord MockRepository
  [entities]
  entity/Repository

  (entity/store [this id-key {id ::entity/id :as entity}]
    (swap! entities assoc-in [id-key id] entity))

  (entity/fetch [this id-key id]
    (future (get-in @entities [id-key id])))

  (entity/exists? [this id-key id]
    (some? (get-in @entities [id-key id]))))


(defrecord MockDispatcher
  [journal counter subscriptions]
  event/Dispatcher

  (subscribe
    [this [event-type handlers]]
    (swap! subscriptions update event-type concat handlers)
    [event-type handlers])

  (unsubscribe
    [this _])

  (dispatch
    [this event]
    (let [event (assoc event ::event/version (swap! counter inc))]
      (->> (select-keys event [::event/kind ::event/type])
           (vals)
           (map @subscriptions)
           (apply union)
           (mapv #(% event)))
      event)))


(defrecord MockJournal
  [trail]
  event/Journal

  (event/store [this event]
    (swap! trail conj event)
    event)

  (event/fetch [this criteria]
    (event/fetch-apply this identity criteria))

  (event/fetch-apply [this fun criteria]
    (future
      (->> criteria
           (reduce (fn [r [k v]] (filter #(= (k %) v) r)) @trail)
           (sort-by ::event/version)
           (fun)))))


(defrecord MockRouter
  [dispatcher journal subscriptions]

  co/Lifecycle

  (start [this]
    (assoc this :subscriptions
           (subscribe-maps dispatcher
                           {::event/message [(store-in journal)]
                            ::event/error [(store-in journal)]})))

  (stop [this]))


(defn boundaries []
  (->MockBoundaries))

(defn repository []
  (->MockRepository (atom {})))

(defn dispatcher []
  (->MockDispatcher nil (atom 0) (atom {})))

(defn journal []
  (->MockJournal (atom [])))

(defn router []
  (->MockRouter nil nil nil))


(defn- strip-canonicals
  [events]
  (->> events
       (map #(dissoc % ::event/id ::event/date ::event/version))
       (return)))


(defn- relay-events
  [events]
  (mdo
    (map-m relay events)
    (>>= (get-events)
         #(return @%))))


(defn scenario
  [& {:keys [using given after relay]}]
  (mdo-within (componad/componad (co/start using))
    given-events <- (relay-events given)
    after-events <- (relay-events after)
    expected <- (>>= (return* relay) strip-canonicals)
    received <- (strip-canonicals (difference (set after-events)
                                              (set given-events)))
    (return (co/stop using))
    (return [(set expected) (set received)])))


(defn test-fn-params
  [params]
  (->> params
       (partition 2)
       (map (fn [[p p-spec]] [`~p `[~(keyword p) ~p-spec]]))
       (zip)))

(defn test-fn-call
  [params body]
  (let [[fn-params fspec-params] (test-fn-params params)]
    `(s/exercise-fn (fn [~@fn-params] (scenario ~@body))
                    10
                    (s/fspec :args (s/cat ~@(flatten fspec-params))))))

(defmacro def-scenario
  [sym params & body]
  `(deftest ~sym
     (->> ~(test-fn-call params body)
          (map second)
          (mapv (fn [[expected# result#]] (is (= expected# result#)))))))
