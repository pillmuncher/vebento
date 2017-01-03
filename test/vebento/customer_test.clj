(ns vebento.customer-test
  (:require [clojure.future
             :refer :all]
            [clojure.test
             :refer :all]
            [clojure.spec
             :as s]
            [com.stuartsierra.component
             :as co]
            [juncture.entity
             :as entity]
            [vebento.core
             :as core]
            [vebento.testing
             :refer [mock-journal mock-dispatcher mock-entity-store]]
            [vebento.product
             :as product]
            [vebento.order
             :as order]
            [vebento.merchant
             :as merchant]
            [vebento.customer
             :as customer]))


(defn test-bench []
  (-> (co/system-map
        :aggregates
        (entity/aggregates core/aggregate-context)
        :entity-store
        (mock-entity-store)
        :dispatcher
        (mock-dispatcher)
        :journal
        (mock-journal)
        :customer
        (customer/->Component nil nil nil nil nil)
        :merchant
        (merchant/->Component nil nil nil nil nil)
        :product
        (product/->Component nil nil nil nil nil)
        :order
        (order/->Component nil nil nil nil nil))
      (co/system-using
        {:customer [:aggregates :dispatcher :journal :entity-store]
         :merchant [:aggregates :dispatcher :journal :entity-store]
         :order [:aggregates :dispatcher :journal :entity-store]
         :product [:aggregates :dispatcher :journal :entity-store]
         :dispatcher [:journal]})
      (co/start)
      (:customer)))