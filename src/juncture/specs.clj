(ns juncture.specs
  (:require [clojure.future
             :refer :all]
            [clojure.spec
             :as s]
            [juncture.util
             :refer [ns-alias]]
            [juncture.event
             :as event
             :refer [def-failure]]))


(ns-alias 'ju 'juncture.core)
(ns-alias 'event 'juncture.event)
(ns-alias 'entity 'juncture.entity)


(s/def ::event/id uuid?)
(s/def ::event/date inst?)
(s/def ::event/type keyword?)
(s/def ::event/kind #{::event/command ::event/message ::event/failure})
(s/def ::event/spec (s/keys :req [::event/kind
                                  ::event/type
                                  ::event/date
                                  ::event/id]))

(s/def ::event/command #(= (::event/kind %) ::event/command))
(s/def ::event/message #(= (::event/kind %) ::event/message))
(s/def ::event/failure #(= (::event/kind %) ::event/failure))


(s/def ::entity/id uuid?)
(s/def ::entity/id-key keyword?)
(s/def ::entity/type keyword?)
(s/def ::entity/kind #{::ju/entity})
(s/def ::entity/spec (s/keys :req [::entity/kind ::entity/type ::entity/id]))


(def-failure ::entity/already-exists
  :req [::entity/id-key
        ::entity/id])

(def-failure ::entity/not-found
  :req [::entity/id-key
        ::entity/id])
