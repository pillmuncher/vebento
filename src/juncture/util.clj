(ns juncture.util
  (:require [clojure.future
             :refer :all])
  (:import  [java.util UUID Date]))


(defn ns-alias
  [a n]
  (->> n (create-ns) (alias a)))


(def uuid #(UUID/randomUUID))
(def inst #(Date.))


(def not-in? (comp not contains?))
