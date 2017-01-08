(ns util
  (:import  [java.util UUID Date]))


(defmacro pull [ns names]
  `(do ~@(for [n names]
           `(def ~n ~(symbol (str ns "/" n))))))


(defn ns-alias
  [a n]
  (->> n (create-ns) (alias a)))


(defn flip [function]
  (fn
    ([] (function))
    ([x] (function x))
    ([x y] (function y x))
    ([x y z] (function z y x))
    ([a b c d] (function d c b a))
    ([a b c d & rest]
     (->> rest
          (concat [a b c d])
          reverse
          (apply function)))))


(def in? (flip contains?))
(def not-in? (comp not in?))

(def zip (partial apply map vector))

(def inst #(Date.))
(def uuid #(UUID/randomUUID))

(def dummy-uuid #uuid "00000000-0000-0000-0000-000000000000")
(def epoch (Date. 0))
