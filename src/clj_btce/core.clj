; Copyright (C) 2014 John. P Hackworth <jph@hackworth.be>
;
; This Source Code Form is subject to the terms of the Mozilla Public
; License, v. 2.0. If a copy of the MPL was not distributed with this
; file, You can obtain one at http://mozilla.org/MPL/2.0/.

(ns clj-btce.core
  (:require [org.httpkit.client :as http]
    [cheshire.core :refer :all]
    [clojure.string :refer [upper-case lower-case join]]
    [pandect.core :refer [sha512-hmac]]))

(def config (atom {:api-key nil 
                   :api-secret nil 
                   :user-agent "clj-btce 0.1.0"
                   :trade-api-url "https://btc-e.com/tapi"
                   :public-api-url "https://btc-e.com/api/2"
                   :keepalive 30000
                   :insecure false
                   :nonce-ms 500}))

(defn sign-params [params]
  (if (nil? (:api-secret @config))
    (throw (Exception. "api-secret not assigned"))
     (sha512-hmac 
    (join "&" 
      (for [[k v] (into {} (filter second params))] 
        (format "%s=%s" k v))) (@config :api-secret))))

(defn nonce 
  "The BTC-E API requires each request to include a unique and incremented nonce integer parameter.
  
  I suspect this is to reduce the possibility of API users from overloading the system with 
  parallel requests. The requirement for each new API request to have an incremented nonce 
  is difficult to share safely across threads.
  
  This function provides an abbreviated time-based nonce. If you need to run parallel requests,
  then you will need to implement your own nonce-management scheme and call post-data directly.
  "
  [] (quot (System/currentTimeMillis) (:nonce-ms @config)))

; Network/Data Handling Functions

(defn fetch-data 
  "fetch-data is used by the unauthenticated, public API calls.
  
  You should not use this directly, as it requires a correctly formatted URL.
  "
  [url]
  (let [options {:method :get 
                 :content-type "application/json"
                 :user-agent (:user-agent @config)
                 :insecure? (:insecure @config) 
                 :keepalive (:keepalive @config)}
        response @(http/get url options)]
    (case (:status response)
      200 (parse-string (response :body) true)
      (throw (Exception. response)))))

(defn post-data 
  "post-data is used for authenticated, trade api calls.
  
  It takes an optional map of parameters as an argument.
  
  You should not use post-data directly, as it requires correctly formatted parameters.
  "
  [& [p]]
  (if (nil? (:api-key @config)) (throw (Exception. "api-key not assigned")))
  
  (let [params (filter second p) 
        options {:method :post
                 :content-type "application/json"
                 :user-agent (:user-agent @config)
                 :insecure? (:insecure @config)
                 :headers {"Key" (:api-key @config) "Sign" (sign-params params) }
                 :form-params params
                 :keepalive (:keepalive @config)}
        response @(http/post (:trade-api-url @config) options)]
          (case (:status response)
            200 (let [body (parse-string (response :body) true)] 
                  (case (:success body)
                    0 (throw (Exception. (:error body)))
                    (:return body)))
            (throw (Exception. response)))))

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
  (let [url (format "%s/%s_%s/fee" (:public-api-url @config) (name currency1) (name currency2))]
    (fetch-data url)))

(defn get-ticker [currency1 currency2]
  (let [url (format "%s/%s_%s/ticker" (:public-api-url @config) (name currency1) (name currency2))]
    (fetch-data url)))

(defn get-trades [currency1 currency2]
  (let [url (format "%s/%s_%s/trades" (:public-api-url @config) (name currency1) (name currency2))]
    (fetch-data url)))

(defn get-depth [currency1 currency2]
 (let [url (format "%s/%s_%s/depth" (:public-api-url @config) (name currency1) (name currency2))]
  (fetch-data url))) 

(defn configure [& {:keys [api-key api-secret]}]
  (if-not (nil? api-key) (swap! config assoc :api-key api-key))
  (if-not (nil? api-secret) (swap! config assoc :api-secret api-secret)))

