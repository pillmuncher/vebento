(ns util
  (:import  [java.util UUID Date]))


(defmacro pull [ns names]
  `(do ~@(for [n names]
           `(def ~n ~(symbol (str ns "/" n))))))


(defn ns-alias
  [a n]
  (->> n (create-ns) (alias a)))


(def not-in? (comp not contains?))


(def uuid #(UUID/randomUUID))
(def inst #(Date.))
