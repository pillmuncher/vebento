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
             :refer [map-m]]
            [util
             :refer [ns-alias zip]]
            [componad
             :refer [within componad return* >>=]]
            [juncture.event
             :as event
             :refer [fetch-apply dispatch subscribe* unsubscribe* store store*]]
            [juncture.entity
             :refer [EntityStore store-entity fetch-entity exists-entity?]]
            [vebento.core
             :refer [get-events raise]]))


(ns-alias 'entity 'juncture.entity)


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


(defrecord MockEntityStore
  [entities]
  entity/EntityStore
  (store-entity [this {id ::entity/id :as entity}]
    (swap! entities assoc id entity))
  (fetch-entity [this id]
    (future (@entities id)))
  (exists-entity? [this id]
    (some? (@entities id))))


(defn mock-entity-store []
  (->MockEntityStore (atom {})))


(defn- strip-canonicals
  [events]
  (->> events
       (map #(dissoc % ::event/id ::event/date ::event/version))
       (return)))


(defn- raise-events
  [events]
  (mdo
    (map-m raise events)
    (>>= (get-events)
         #(strip-canonicals @%))))


(defn scenario
  [& {:keys [using given after raise]}]
  (within (componad using)
    given-events <- (raise-events given)
    after-events <- (raise-events after)
    raise-events <- (>>= (return* raise) strip-canonicals)
    (return [(set raise-events) (difference (set after-events)
                                            (set given-events))])))


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
