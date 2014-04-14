; Copyright (C) 2014 John. P Hackworth <jph@hackworth.be>
;
; This Source Code Form is subject to the terms of the Mozilla Public
; License, v. 2.0. If a copy of the MPL was not distributed with this
; file, You can obtain one at http://mozilla.org/MPL/2.0/.

(ns clj-btce.core
  (:require [clj-http.client :as client]
            [schema.core :as s]
            [medley.core :refer [remove-vals]]
            [clj-btce.validation :refer :all]
            [aleph.http.client-middleware :refer [generate-query-string]]
            [pandect.core :refer [sha512-hmac]]))

(def nonce-ms (atom 500))
(def trade-api-url (atom "https://btc-e.com/tapi"))
(def public-api-url (atom "https://btc-e.com/api/2"))

; Utility Functions

(defn sign-params [params api-secret]
  (if (nil? api-secret)
    (throw (Exception. "api-secret not assigned"))
    (sha512-hmac (generate-query-string params) api-secret)))
    
(defn get-nonce
  "The BTC-E API requires each request to include a unique and incremented nonce integer parameter.

  The nonce is tracked per-API key. If your last request was 100 (24 hours ago), it'll expect the
  next request to be 100+.

  This function provides an abbreviated time-based nonce. If you need to run parallel requests,
  then you will need to implement your own nonce-management scheme and call post-data directly.
  "
  [] (quot (System/currentTimeMillis) @nonce-ms))

; API Interaction

(defn api-action [opts]
  (client/request
    (merge {:insecure? false 
            :auto-transform true 
            :as :json} opts)))

(defn public-api-call 
  [path & [opts]] 
  (let [response (api-action (merge {:method :get 
                                     :url (format "%s/%s" @public-api-url path)} 
                                    opts))]
    (case (:status response) 
      200 (:body response) 
      (throw (Exception.) response))))

(defn trade-api-call 
  [{:keys [api-key api-secret] :as account} 
   {:keys [nonce] :as params} 
   & [opts]]
   
  (let [nonce (if-not (nil? nonce) nonce (get-nonce))
        params (->> {:nonce nonce}
                    (merge params)
                    (remove-vals nil?))
        headers {"Key" api-key
                 "Sign" (sign-params params api-secret)}
        response (api-action (merge {:method :post
                                     :url @trade-api-url 
                                     :form-params params 
                                     :headers headers} opts))]
    
    (case (:status response) 
      200 (:body response) 
      (throw (Exception.) response))))

; Public API Functions

(s/defn get-ticker
        "Get the public ticker information for the supplied pair.

        Parameters:
        - pair (required): Must be a valid pair (see validation.clj for list)
        - http-options (optional): specify parameters such as keepalive, user-agent"
        [pair :- ValidPairs
         & [http-options]]
        (public-api-call (format "%s/%s" pair "ticker") http-options))

(s/defn get-trades
        "Get the public trades information for the supplied pair.

        Parameters:
        - pair (required): Must be a valid pair (see validation.clj for list)
        - http-options (optional): specify parameters such as keepalive, user-agent

        Trade maps are converted into Trade records for performance.
        "
        [pair :- ValidPairs
         & [http-options]]
        (public-api-call (format "%s/%s" pair "trades") http-options))

(s/defn get-depth
        "Get the public depth information for the supplied pair.

        Parameters:
        - pair (required): Must be a valid pair (see validation.clj for list)
        - http-options (optional): specify parameters such as keepalive, user-agent"
        [pair :- ValidPairs
         & [http-options]]
        (public-api-call (format "%s/%s" pair "depth") http-options))

(s/defn get-fee
        "Get the public fee information for the supplied pair.

        Parameters:
        - pair (required): Must be a valid pair (see validation.clj for list)
        - http-options (optional): specify parameters such as keepalive, user-agent"
        [pair :- ValidPairs
         & [http-options]]
        (public-api-call (format "%s/%s" pair "fee") http-options))

; Trading API Functions

(s/defn get-info 
  [account :- AccountValidation
   & [{:keys [nonce http-options] :as options}]] 
  (trade-api-call account {:method "getInfo" :nonce nonce} http-options))

(s/defn ^:always-validate create-order
        "Create a buy/sell order.

        Parameters:
        - account (required): This is a map containing :api-key and :api-secret keys
        - order (required): This is a map containing :pair, :type, :rate and :amount keys
        - http-options (optional): Specify custom keepalive, user-agent"
        [account :- AccountValidation
         order :- OrderValidation
         & [{:keys [nonce http-options] :as options}]]
        (trade-api-call account (merge order {:method "Trade" :nonce nonce}) http-options))

(s/defn ^:always-validate cancel-order
        "Cancel an order.

        Parameters:
        - account (required): This is a map containing :api-key and :api-secret keys
        - order_id (required): This is the order id to cancel
        - http-options (optional): Specify custom keepalive, user-agent"
        [account :- AccountValidation
         order_id :- s/Num
         & [{:keys [nonce http-options] :as options}]]
        (trade-api-call account {:order_id order_id :method "CancelOrder" :nonce nonce} http-options))

(s/defn get-trade-history
        "Retrieve completed trades history."
        [{:keys [account history-filter] :as params} :- HistoryValidation
         & [{:keys [nonce http-options] :as options}]]
        (trade-api-call account (merge history-filter {:method "TradeHistory" :nonce nonce}) http-options))

(s/defn get-transaction-history
        "Retrieve transaction history."
        [{:keys [account history-filter] :as params} :- HistoryValidation
         & [{:keys [nonce http-options] :as options}]]
        (trade-api-call account (merge history-filter {:method "TransHistory" :nonce nonce}) http-options))

(s/defn get-active-orders
        "Retrieves open orders for currency pair."
        [account :- AccountValidation
         pair :- ValidPairs
         & [{:keys [nonce http-options] :as options}]]
        (trade-api-call account {:pair pair :method "ActiveOrders" :nonce nonce} http-options))
