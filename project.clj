(defproject clj-btce "0.3.0"
  :description "API support for BTC-E"
  :author {:email "jph@hackworth.be" :web "https://hackworth.be"}
  :url "https://github.com/jphackworth/clj-btce"
  :license {:name "Mozilla Public License Version 2.0"
  :url "https://www.mozilla.org/MPL/2.0/"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                  [pandect "0.3.0"]
                  [prismatic/schema "0.2.1"]
                  [medley "0.1.5"]
                  [aleph "0.3.2"]
                  [clj-http "0.9.1"]
                  [me.raynes/fs "1.4.4"]]
  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[org.clojure/tools.namespace "0.2.4"]
                                  [criterium "0.4.3"]
]}})
