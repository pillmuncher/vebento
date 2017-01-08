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
             :refer [fetch-apply dispatch subscribe-maps unsubscribe* store-in]]
            [juncture.entity
             :as entity
             :refer [Boundary]]
            [vebento.core
             :refer [raise EntityStore store-entity fetch-entity exists-entity?
                     get-events]]))


(defrecord Componad
  [boundaries entities trail counter subscriptions]

  Boundary

  (register [this boundary-keys]
    (swap! boundaries union boundary-keys))

  (unregister [this boundary-keys]
    (swap! boundaries #(difference boundary-keys %)))

  (run [this boundary-keys fun]
    (fun))

  EntityStore

  (store-entity [this id-key {id ::entity/id :as entity}]
    (swap! entities assoc-in [id-key id] entity))

  (fetch-entity [this id-key id]
    (future (get-in @entities [id-key id])))

  (exists-entity? [this id-key id]
    (some? (get-in @entities [id-key id])))

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
    event)

  event/Dispatcher

  (subscribe
    [this [event-type handlers]]
    (swap! subscriptions update event-type concat handlers)
    (vector event-type handlers))

  (unsubscribe
    [this _] this)

  (dispatch
    [this event]
    (let [event (assoc event ::event/version (swap! counter inc))]
      (->> (select-keys event [::event/kind ::event/type])
           (vals)
           (map @subscriptions)
           (apply union)
           (mapv #(% event)))
      event))

  co/Lifecycle

  (start [this]
    (subscribe-maps this
                    {::event/message
                     [(store-in this)]
                     ::event/failure
                     [(store-in this)]})
    this)

  (stop [this]
    (assoc this subscriptions nil)))


(defn component []
  (->Componad (atom #{})
              (atom {})
              (atom [])
              (atom 0)
              (atom {})))


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
         #(return @%))))


(defn scenario
  [& {:keys [using given after raise]}]
  (within (componad using)
    given-events <- (raise-events given)
    after-events <- (raise-events after)
    expected <- (>>= (return* raise) strip-canonicals)
    received <- (strip-canonicals (difference (set after-events)
                                              (set given-events)))
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
