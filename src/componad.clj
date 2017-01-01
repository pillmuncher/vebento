(ns componad
  (:require [clojure.future
             :refer :all]
            [monads.core
             :as monad
             :refer [mdo return fail ask asks get-state]]
            [monads.types
             :refer [fst either]]
            [monads.util
             :refer [mwhen map-m sequence-m]]
            [monads.rws
             :as rws]
            [monads.error
             :as error]))


(def componad-m (error/t rws/m))


(defn run-componad
  [computation & {:keys [state component]}]
  (rws/run-rws-t componad-m computation state component))


(defn return*
  [values]
  (map-m return values))

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


(defn componad
  ([co]
   (componad co nil))
  ([co st]
   (fn [computation]
     (extract (run-componad computation
                            :state st
                            :component co)))))


(defn system
  ([co]
   (system co nil))
  ([co st]
   (fn [computation]
     (m-extract (run-componad computation
                              :state st
                              :component co)))))


(defn component
  [co-key]
  (fn [computation]
    (>>= (sequence-m [ask get-state])
         (fn [[co st]]
           (m-extract (run-componad computation
                                    :state st
                                    :component (co-key co)))))))


(defmacro within
  [environment & computations]
  `(~environment (mdo ~@computations)))


(defmacro f-return [& body]
  `(return (future ~@body)))


(defmacro mdo-future [& computations]
  `(>>= (sequence-m [ask get-state])
        (fn [[co# st#]]
          (let [result# (future
                          (m-extract
                            (run-componad (mdo ~@computations)
                                          :state st#
                                          :component co#)))]
            (return
              (reify clojure.lang.IDeref
                (deref [me] @result#)))))))


(defmacro mdo-await*
  [& computations]
  (let [qs (for [c computations]
             `(mdo-future ~c))]
    `(>>= (sequence-m [~@qs])
          #(map-m deref %))))


(defmacro munless
  "Execute the computation acc if p is falsy."
  [p acc]
  `(if ~p
     ~(return nil)
     ~acc))


(defmacro f-mwhen
  [& {:as expression-action-pairs}]
  (let [qs (for [[e a] expression-action-pairs]
             `(mdo-future (mwhen ~e ~a)))]
    `(>>= (sequence-m [~@qs])
          #(map-m deref %))))

(defmacro f-munless
  [& {:as expression-action-pairs}]
  (let [qs (for [[e a] expression-action-pairs]
             `(mdo-future (munless ~e ~a)))]
    `(>>= (sequence-m [~@qs])
          #(map-m deref %))))
