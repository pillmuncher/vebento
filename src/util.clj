(ns util
  (:import  [java.util UUID Date]))


(defmacro pull [ns name-map]
  `(do ~@(for [[there here] name-map]
           `(def ~here ~(symbol (str ns "/" there))))))


(defn ns-alias
  [a n]
  (->> n (create-ns) (alias a)))


(def not-in? (comp not contains?))


(def uuid #(UUID/randomUUID))
(def inst #(Date.))
