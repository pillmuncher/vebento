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
            [componad
             :refer [within]]
            [vebento.util
             :refer [ns-alias not-in?]]
            [vebento.core
             :refer [def-aggregate aggregate publish execute fail-with
                     fail-if-exists fail-unless-exists f-mwhen get-entity]]
            [juncture
             :refer [def-command def-message def-failure def-entity
                     subscribe unsubscribe store create transform]]
            [vebento.specs
             :as specs]))
