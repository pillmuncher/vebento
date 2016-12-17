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
(s/def ::event/kind #{::ju/command ::ju/message ::ju/failure})
(s/def ::event/spec (s/keys :req [::event/kind
                                  ::event/type
                                  ::event/date
                                  ::event/id]))

(s/def ::ju/event ::event/spec)

(s/def ::ju/command #(= (::event/kind %) ::ju/command))
(s/def ::ju/message #(= (::event/kind %) ::ju/message))
(s/def ::ju/failure #(= (::event/kind %) ::ju/failure))


(s/def ::entity/id uuid?)
(s/def ::entity/id-key keyword?)
(s/def ::entity/type keyword?)
(s/def ::entity/kind #{::ju/entity})
(s/def ::entity/spec (s/keys :req [::entity/kind ::entity/type ::entity/id]))

(s/def ::ju/entity #(= (::entity/kind %) ::ju/entity))



(def-failure ::entity/already-exists
  :req [::entity/id-key
        ::entity/id])

(def-failure ::entity/not-found
  :req [::entity/id-key
        ::entity/id])
