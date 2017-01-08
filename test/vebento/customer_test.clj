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
             :as testing]
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
        :componad
        (testing/component)
        :customer
        (customer/->Component nil nil)
        :merchant
        (merchant/->Component nil nil)
        :product
        (product/->Component nil nil)
        :order
        (order/->Component nil nil))
      (co/system-using
        {:product [:componad]
         :order [:componad]
         :merchant [:componad]
         :customer [:componad]})
      (co/start)))
