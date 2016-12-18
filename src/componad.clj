(ns componad
  (:require [clojure.future
             :refer :all]
            [monads.core
             :as monad
             :refer [mdo return fail ask]]
            [monads.types
             :refer [fst either]]
            [monads.rws
             :as rws]
            [monads.error
             :as error]))


(def error-rws (error/t rws/m))


(defmacro within
  [environment & computations]
  `(~environment (mdo ~@computations)))


(defn extract
  [mval]
  (either identity identity (fst mval)))


(defn system
  [env]
  (fn [computation]
    (rws/run-rws-t error-rws computation nil env)))


(defn >>=
  [m & mfs]
  (reduce monad/>>= m mfs))
