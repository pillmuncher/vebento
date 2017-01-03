(ns vebento.customer.removes-item-from-cart-test
  (:require [clojure.future
             :refer :all]
            [clojure.test
             :refer :all]
            [juncture.event
             :refer [command message failure]]
            [vebento.testing
             :refer [def-scenario]]
            [vebento.customer-test
             :refer [test-bench]]))
