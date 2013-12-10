; Copyright (C) 2013 John. P Hackworth <jph@hackworth.be>
;
; This Source Code Form is subject to the terms of the Mozilla Public
; License, v. 2.0. If a copy of the MPL was not distributed with this
; file, You can obtain one at http://mozilla.org/MPL/2.0/.

(ns clj-btce.core
  (:require [org.httpkit.client :as http]
    [clojure.data.json :as json]
    [clj-time.format :as tf]  
    [clj-time.core :as t]
    [clj-time.local :as tl]
    [clj-time.coerce :as coerce]
    [clj-time.periodic :refer [periodic-seq]]
    [clojure.data.csv :as csv]
    [clojure.java.io :as io])
  (:use 
    [clojure.string :only [upper-case lower-case join]]
    [pandect.core]
    [clojure.tools.namespace.repl :only (refresh)]

    ))

(def credentials (atom {:api-key nil :api-secret nil}))

(def api-url "https://btc-e.com/tapi")

(defn sign-params [params]
  (sha512-hmac 
    (join "&" 
      (for [[k v] (into {} (filter second params))] 
        (format "%s=%s" k v))) (@credentials :api-secret)))

(defn nonce 
  "The BTC-E API requires each request to include a unique and incremented nonce integer parameter.
  
  I suspect this is to reduce the possibility of API users from overloading the system with 
  parallel requests. The requirement for each new API request to have an incremented nonce 
  is difficult to share safely across threads.
  
  This function provides an abbreviated time-based nonce. If you need to run parallel requests,
  then you will need to implement your own nonce-management scheme and call post-data directly.
  "
  [] (quot (System/currentTimeMillis) 500))

; Network/Data Handling Functions

(defn fetch-data 
  "fetch-data is used by the unauthenticated, public API calls.
  
  You should not use this directly, as it requires a correctly formatted URL.
  "
  [url]  
  (def options {:method :get 
    :content-type "application/json"
    :user-agent "clj-btce 0.0.1"
    :insecure? false 
    :keepalive 30000})
  (def response @(http/get url options))
  (case (response :status)
    200 (json/read-str (response :body) :key-fn keyword)
    nil))

(defn post-data 
  "post-data is used for authenticated, trade api calls.
  
  It takes an optional map of parameters as an argument.
  
  You should not use post-data directly, as it requires correctly formatted parameters.
  "
  [& [params]]
  
  (let [p (filter second params)]
    (let [options {:method :post
      :content-type "application/json"
      :user-agent "clj-btce 0.0.1"
      :insecure? false
      :headers {"Key" (@credentials :api-key) "Sign" (sign-params p) }
      :form-params p
      :keepalive 30000}]
      (let [response @(http/post api-url options)]
        (case (response :status)
          200 (json/read-str (response :body) :key-fn keyword)
          nil)))))

; Trading API - https://btc-e.com/api/documentation

(defn get-info [] 
  (post-data {"method" "getInfo" "nonce" (nonce)}))

(defn get-transaction-history
  [& {:keys [from limit from_id end_id order since end]}]
  (post-data {"method" "TransHistory" "nonce" (nonce) "from" from "count" limit "from_id" 
   from_id "end_id" end_id "order" order "since" since "end" end}))


(defn get-trade-history
  [& {:keys [from limit from_id end_id order since end pair]}]
  (post-data {"method" "TradeHistory" "nonce" (nonce) "from" from "count" limit "from_id" from_id
    "end_id" end_id "order" order "since" since "end" end "pair" pair}))

(defn get-active-orders [ & [pair]] 
  (post-data {"method" "ActiveOrders" "nonce" (nonce) "pair" pair})) 
   

(defn create-trade [currency1 currency2 type rate amount]
  (post-data {"method" "Trade" "nonce" (nonce) "pair" (format "%s_%s" (name currency1) (name currency2)) "type" type 
      "rate" rate "amount" amount}))

(defn cancel-order [orderid]
  (post-data {"method" "CancelOrder" "order_id" orderid "nonce" (nonce)}))


; Public API - https://btc-e.com/page/2

(defn get-fee [currency1 currency2]
  (let [url (format "https://btc-e.com/api/2/%s_%s/fee" (name currency1) (name currency2))]
    (fetch-data url)))

(defn get-ticker [currency1 currency2]
  (let [url (format "https://btc-e.com/api/2/%s_%s/ticker" (name currency1) (name currency2))]
    (fetch-data url)))

(defn get-trades [currency1 currency2]
  (let [url (format "https://btc-e.com/api/2/%s_%s/trades" (name currency1) (name currency2))]
    (fetch-data url)))

(defn get-depth [currency1 currency2]
 (let [url (format "https://btc-e.com/api/2/%s_%s/depth" (name currency1) (name currency2))]
  (fetch-data url))) 

(defn configure [& {:keys [api-key api-secret]}]
  (if-not (nil? api-key) (swap! credentials assoc :api-key api-key))
  (if-not (nil? api-secret) (swap! credentials assoc :api-secret api-secret)))

