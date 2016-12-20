(ns componad
  (:require [clojure.future
             :refer :all]
            [monads.core
             :as monad
             :refer [mdo return fail ask]]
            [monads.types
             :refer [fst either]]
            [monads.util
             :refer [mwhen]]
            [monads.identity
             :as ident]
            [monads.rws
             :as rws]
            [monads.error
             :as error]))


(def error-rws (error/t rws/m))


(defn >>=
  [m & mfs]
  (reduce monad/>>= m mfs))


(defn extract
  [mval]
  (either identity identity (fst mval)))


(defmacro within
  [environment & computations]
  `(~environment (mdo ~@computations)))


(defn system
  ([co]
   (system co nil))
  ([co st]
   (fn [computation]
     (extract (rws/run-rws-t error-rws computation st co)))))


(defn component
  [co]
  (fn [computation]
    (>>= ask
         #(within (system (co %)) computation))))



(defmacro m-future [& body]
  `(return (future ~@body)))


(defmacro m-when
  [m-condition computation]
  `(>>= ~m-condition #(mwhen % ~computation)))

(defmacro m-unless
  [m-condition computation]
  `(>>= ~m-condition #(mwhen (not %) ~computation)))
