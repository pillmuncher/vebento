(ns vebento.entity.product
  (:require [clojure.future
             :refer :all]
            [clojure.set
             :refer [union intersection]]
            [clojure.spec
             :as s]
            [com.stuartsierra.component
             :as co]
            [monads.util
             :refer [mwhen]]
            [util
             :refer [ns-alias not-in?]]
            [juncture.event
             :as event
             :refer [def-command def-message def-failure
                     subscribe unsubscribe store]]
            [juncture.entity
             :as entity
             :refer [def-entity create transform]]
            [componad
             :refer [within]]
            [vebento.core
             :refer [def-aggregate aggregate publish execute fail-with
                     fail-if-exists fail-unless-exists f-mwhen get-entity]]
            [vebento.specs
             :as specs]))
