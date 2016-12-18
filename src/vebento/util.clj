(ns vebento.util
  (:require [clojure.future
             :refer :all]))


(defn ns-alias
  [a n]
  (->> n (create-ns) (alias a)))


(def not-in? (comp not contains?))
