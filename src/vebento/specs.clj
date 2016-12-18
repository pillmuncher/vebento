(ns vebento.specs
  (:require [clojure.future
             :refer :all]
            [clojure.spec
             :as s]
            [util
             :refer [ns-alias]]))



(ns-alias 'entity 'juncture.entity)

(ns-alias 'customer 'vebento.entity.customer)
(ns-alias 'retailer 'vebento.entity.retailer)
(ns-alias 'order 'vebento.entity.order)
(ns-alias 'product 'vebento.entity.product)



(s/def ::keyword keyword?)
(s/def ::string string?)
(s/def ::int integer?)
(s/def ::map map?)
(s/def ::set set?)
(s/def ::int integer?)
(s/def ::bool boolean?)
(s/def ::date inst?)
(s/def ::uuid uuid?)

(s/def ::pos pos?)
(s/def ::non-neg (comp neg? not))
(s/def ::pos-int (s/and ::int ::pos))
(s/def ::non-neg-int (s/and ::int ::non-neg))

(s/def ::id ::uuid)
(s/def ::aggregate ::keyword)
(s/def ::failure ::keyword)

(s/def ::amount ::non-neg-int)
(s/def ::name ::string)
(s/def ::number-of-persons ::amount)
(s/def ::number-of-dishes ::amount)
(s/def ::time-of-day ::string)
(s/def ::is-recurrent ::bool)
(s/def ::first-name ::string)
(s/def ::last-name ::string)
(s/def ::street ::string)
(s/def ::zipcode ::string)
(s/def ::city ::string)
(s/def ::email ::string)
(s/def ::phone ::string)
(s/def ::schedule ::set)
(s/def ::payment-method ::string)
(s/def ::paid ::bool)
(s/def ::shipped ::bool)


(s/def ::address
  (s/keys :req [::first-name
                ::last-name
                ::street
                ::zipcode
                ::city
                ::email]
          :opt [::phone]))

(s/def ::items set?)
