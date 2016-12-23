(ns util
  (:import  [java.util UUID Date]))


(defmacro pull [ns names]
  `(do ~@(for [n names]
           `(def ~n ~(symbol (str ns "/" n))))))


(defn ns-alias
  [a n]
  (->> n (create-ns) (alias a)))


(def zip (partial apply map vector))
(def not-in? (comp not contains?))


(def inst #(Date.))
(def uuid #(UUID/randomUUID))

(def dummy-uuid #uuid "00000000-0000-0000-0000-000000000000")
(def epoch (Date. 0))
