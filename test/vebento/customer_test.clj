(ns vebento.customer-test
  (:require [clojure.future
             :refer :all]
            [com.stuartsierra.component
             :as co]
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
        :boundaries (testing/boundaries)
        :repository (testing/repository)
        :journal (testing/journal)
        :dispatcher (testing/dispatcher)
        :customer (customer/->Component nil nil nil nil nil)
        :merchant (merchant/->Component nil nil nil nil nil)
        :product (product/->Component nil nil nil nil nil)
        :order (order/->Component nil nil nil nil nil))
      (co/system-using
        {:dispatcher [:journal]
         :product [:boundaries :repository :journal :dispatcher]
         :order [:boundaries :repository :journal :dispatcher]
         :merchant [:boundaries :repository :journal :dispatcher]
         :customer [:boundaries :repository :journal :dispatcher]})
      (co/start)))
