(ns vebento.core-test
  (:require [clojure.future
             :refer :all]
            [clojure.test
             :refer :all]
            [vebento.core
             :as vebento
             :refer :all]
            [vebento.entity.product-test
             :as product-test
             :refer :all]
            [vebento.entity.order-test
             :as order-test
             :refer :all]
            [vebento.entity.retailer-test
             :as retailer-test
             :refer :all]
            [vebento.entity.customer-test
             :as customer-test
             :refer :all]))
