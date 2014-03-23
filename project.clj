(defproject clj-btce "0.2.1"
  :description "API support for BTC-E"
  :author {:email "jph@hackworth.be" :web "https://hackworth.be"}
  :url "https://github.com/jphackworth/clj-btce"
  :license {:name "Mozilla Public License Version 2.0"
  :url "https://www.mozilla.org/MPL/2.0/"}
  :dependencies [[org.clojure/clojure "1.5.1"]
  [http-kit "2.1.16"]
  [pandect "0.3.0"]
  [cheshire "5.3.1"]
  [prismatic/schema "0.2.1"]
  [medley "0.1.5"]]
  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[org.clojure/tools.namespace "0.2.4"]]}})
