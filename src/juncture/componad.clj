(ns juncture.componad
  (:require [clojure.future
             :refer :all]
            [monads.core
             :as monad
             :refer [mdo local]]
            [monads.types
             :refer [fst]]
            [monads.rws
             :as rws]
            [monads.error
             :as error]))


(def error-rws (rws/t error/m))


(defmacro within
  [environment & computations]
  `(~environment (mdo ~@computations)))


(defn system
  [env]
   (fn [computation]
     (rws/run-rws-t error-rws computation nil env)))


(defn component
  [component-selector & extra-keys]
  (fn [computation]
    (local #(-> % component-selector (merge (select-keys % extra-keys)))
      computation)))


(defn >>=
  [m & mfs]
  (reduce monad/>>= m mfs))
