(ns componad
  (:require [clojure.future
             :refer :all]
            [monads.core
             :as monad
             :refer [mdo return fail ask asks get-state]]
            [monads.types
             :refer [fst either]]
            [monads.util
             :refer [mwhen sequence-m]]
            [monads.identity
             :as ident]
            [monads.rws
             :as rws]
            [monads.error
             :as error]))


(defn >>=
  [m & mfs]
  (reduce monad/>>= m mfs))


(defmacro m-when
  [m-condition computation]
  `(>>= ~m-condition #(mwhen % ~computation)))

(defmacro m-unless
  [m-condition computation]
  `(>>= ~m-condition #(mwhen (not %) ~computation)))


(defn extract
  [result]
  (either identity identity (fst result)))

(defn m-extract
  [result]
  (either fail return (fst result)))


(def error-rws (error/t rws/m))
(def run-error-rws (partial rws/run-rws-t error-rws))


(defn system
  ([co]
   (system co nil))
  ([co st]
   (fn [computation]
     (m-extract (run-error-rws computation st co)))))


(defn component
  [co-key]
  (fn [computation]
    (>>= (sequence-m [get-state ask])
         (fn [[st co]]
           (m-extract (run-error-rws computation st (co-key co)))))))


(defmacro within
  [environment & computations]
  `(~environment (mdo ~@computations)))


(defmacro mdo-future [& computations]
  `(>>= (sequence-m [get-state ask])
        (fn [[st# co#]]
          (let [result# (future
                          (m-extract
                            (run-error-rws (mdo ~@computations) st# co#)))]
            (return
              (reify clojure.lang.IDeref
                (deref [me] @result#)))))))


(defmacro f-return [& body]
  `(return (future ~@body)))


(within (system {:foo 123 :bar {:foo 456}})
  x <- (asks :foo)
  f <- (mdo-future
         (within (component :bar)
           x <- (asks :foo)
           (fail x)))
  y <- @f
  (return (+ x y)))


(within (system {:foo 123 :bar {:foo 456}})
  x <- (asks :foo)
  y <- (within (component :bar)
         (asks :foo))
  (return (+ x y)))
