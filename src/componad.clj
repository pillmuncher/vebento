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


;(defmacro mdo-future-body
  ;[sys computations]
  ;`(try
     ;(either #(vector :failure %)
             ;#(vector :success %)
             ;(fst (within (system ~sys) ~@computations)))
     ;(catch Exception e#
       ;(vector :error e#))))


;(defmacro mdo-future
  ;[& computations]
  ;`(>>= ask
        ;#(let [f# (future (mdo-future-body % ~computations))]
           ;(return (reify clojure.lang.IDeref
                     ;(deref [ref]
                       ;(let [[state# result#] @f#]
                         ;(case state#
                              ;:success (return result#)
                              ;:failure (fail result#)
                              ;:error (throw result#)))))))))

(defn >>=
  [m & mfs]
  (reduce monad/>>= m mfs))
