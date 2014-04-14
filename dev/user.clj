(ns user
  (:require [clj-btce.core :refer :all]
            [clj-btce.currencies :refer :all]
            [clj-btce.helpers :refer :all]
            [clojure.tools.namespace.repl :refer [refresh]]
            [clojure.repl :refer :all]
            [schema.core :as s]
            [schema.macros :as sm]
            [clj-http.client :as client]
            [clojure.edn :as edn]
            [clojure.pprint :refer :all]
            [clojure.string :as string])
  (:use criterium.core)
  (:import [java.io File IOException FileNotFoundException]
           [java.nio.file Files Path LinkOption]
           [java.nio.file.attribute PosixFilePermissions PosixFileAttributes]))

(defn load-config [& [filename]] 
  (let [filename (if-not (nil? filename) 
                   filename
                   (str (System/getenv "HOME") "/.btce.conf"))]
      (edn/read-string (slurp filename))))

(def account (load-config))

(def test-buy-order1 {:pair "ltc_btc"
                      :type "buy"
                      :rate 0.001
                      :amount 1})
(def test-sell-order1 {:pair "ltc_btc"
                       :type "sell"
                       :rate 1
                       :amount 1})
