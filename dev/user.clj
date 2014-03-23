(ns user
  (:require [clj-btce.core :refer :all]
            [clj-btce.currencies :refer :all]
            [clj-btce.helpers :refer :all]
            [clojure.tools.namespace.repl :refer [refresh]]
            [clojure.repl :refer :all]
            [schema.core :as s]
            [schema.macros :as sm]
            [cheshire.core :refer :all]
            [clojure.pprint :refer :all]
            [clojure.string :as string]))

(def acct {:api-secret (clojure.string/trim-newline (slurp "/home/user/.btce.secret"))
                         :api-key (clojure.string/trim-newline (slurp "/home/user/.btce.key"))})


