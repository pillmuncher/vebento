(ns componad
  (:require [clojure.future
             :refer :all]
            [monads.core
             :as monad
             :refer [mdo return fail >>=]]
            [monads.types
             :refer [fst either]]
            [monads.rws
             :as rws]
            [monads.error
             :as error]))


(defn >>=
  [m & mfs]
  (reduce monad/>>= m mfs))


(def error-rws (error/t rws/m))


(defn extract
  [mval]
  (fst mval))

(defn m-extract
  [mval]
  (either fail return (fst mval)))


(defmacro within
  [environment & computations]
  `(~environment (mdo ~@computations)))


(defn system
  [env]
  (fn [computation]
    (rws/run-rws-t error-rws computation nil env)))
