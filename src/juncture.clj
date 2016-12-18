(ns juncture
  (:require [clojure.future
             :refer :all]
            [juncture.core
             :as core
             :refer :all]
            [juncture.event
             :as event
             :refer :all]
            [juncture.entity
             :as entity
             :refer :all]))


(defmacro pull [ns name-map]
  `(do ~@(for [[there here] name-map]
           `(def ~here ~(symbol (str ns "/" there))))))


(pull juncture.core {})
(pull juncture.event {})
(pull juncture.entity {})
