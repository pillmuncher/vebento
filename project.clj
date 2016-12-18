(defproject vebento "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [clojure-future-spec "1.9.0-alpha14"]
                 [org.clojure/test.check "0.9.0"] ;; only if you need generators
                 [org.clojure/core.async "0.2.395"]
                 ;[org.clojure/tools.trace "0.7.9"]
                 [com.stuartsierra/component "0.3.2"]
                 [bwo/monads "0.2.2"]
                 [clojurewerkz/money "1.9.0"]])