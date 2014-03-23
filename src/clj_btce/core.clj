; Copyright (C) 2014 John. P Hackworth <jph@hackworth.be>
;
; This Source Code Form is subject to the terms of the Mozilla Public
; License, v. 2.0. If a copy of the MPL was not distributed with this
; file, You can obtain one at http://mozilla.org/MPL/2.0/.

(ns clj-btce.core
  (:require [org.httpkit.client :as http]
            [cheshire.core :refer :all]
            [clojure.string :refer [upper-case lower-case join]]
            [clj-btce.currencies :refer :all]
            [clj-btce.helpers :refer :all]
            [schema.core :as s]
            [clj-btce.validation :refer :all]
            [pandect.core :refer [sha512-hmac]]))

(s/defrecord Trade
             [date :- Long
              price :- BigDecimal
              amount :- BigDecimal
              tid :- Long
              price_currency :- [s/Str]
              item :- [s/Str]
              trade_type :- [s/Str]])

(s/defrecord Ticker
             [last :- BigDecimal
              vol_cur :- BigDecimal
              low :- BigDecimal
              updated :- Long
              buy :- BigDecimal
              vol :- BigDecimal
              sell :- BigDecimal
              avg :- BigDecimal
              server_time :- Long
              high :- BigDecimal])

(def default-http-options {:content-type "application/json"
                           :user-agent "clj-btce 0.2.0"
                           :insecure? false
                           :keepalive 300})

(def nonce-ms (atom 500))
(def trade-api-url (atom "https://btc-e.com/tapi"))
(def public-api-url (atom "https://btc-e.com/api/2"))

; Utility Functions

(defn sign-params [params api-secret]
  (if (nil? api-secret)
    (throw (Exception. "api-secret not assigned"))
    (sha512-hmac
     (join "&"
           (for [[k v] (into {} (filter second params))]
             (format "%s=%s" k v))) api-secret)))

(defn get-nonce
  "The BTC-E API requires each request to include a unique and incremented nonce integer parameter.

  The nonce is tracked per-API key. If your last request was 100 (24 hours ago), it'll expect the
  next request to be 100+.

  This function provides an abbreviated time-based nonce. If you need to run parallel requests,
  then you will need to implement your own nonce-management scheme and call post-data directly.
  "
  [] (quot (System/currentTimeMillis) @nonce-ms))

(defn load-account [] {:api-secret (clojure.string/trim-newline (slurp "/home/user/.btce.secret"))
                         :api-key (clojure.string/trim-newline (slurp "/home/user/.btce.key"))})

; API Interaction

(defn api-call [http-options & [request-type]]
  (http/request http-options
                (fn [{:keys [status body error] :as response}]
                  (if error
                    (-> "Request failed: %s"
                        (format error)
                        (Exception.)
                        (throw))

                    (case status
                      200 (if-not (nil? request-type)
                            (case request-type
                              "ticker" (map->Ticker (get (parse-string body true) :ticker))
                              "trades" (map #(map->Trade %) (parse-string body true))
                              (parse-string body true)
                              )
                            (parse-string body true))
                      (throw (Exception. response)))))))

(s/defn public-api-call
        [pair :- ValidPairs
         endpoint :- (s/enum "ticker" "trades" "depth" "fee")
         & [http-options]]
        (let [url (format "%s/%s/%s" @public-api-url pair endpoint)
              http-options (-> (merge default-http-options http-options)
                               (assoc :method :get)
                               (assoc :url url))]
          (api-call http-options endpoint)))

(s/defn trade-api-call
        [account :- AccountValidation
         params
         & [http-options]]

        (let [params (-> {:nonce (get-nonce)}
                         (merge params)
                         (->> (map-keys name)
                              (filter second)))
              headers {"Key"  (get account :api-key)
                       "Sign" (sign-params params (get account :api-secret))}
              http-options (-> (merge default-http-options http-options)
                               (assoc :method :post)
                               (assoc :url @trade-api-url)
                               (assoc :headers headers)
                               (assoc :form-params params))]
          (api-call http-options)))

; Public API Functions

(s/defn ^:always-validate get-ticker
        "Get the public ticker information for the supplied pair.

        Parameters:
        - pair (required): Must be a valid pair (see validation.clj for list)
        - http-options (optional): specify parameters such as keepalive, user-agent"
        [pair :- ValidPairs
         & [http-options :- HTTPOptionsValidation]]
        (public-api-call pair "ticker" http-options))

(s/defn ^:always-validate get-trades
        "Get the public trades information for the supplied pair.

        Parameters:
        - pair (required): Must be a valid pair (see validation.clj for list)
        - http-options (optional): specify parameters such as keepalive, user-agent

        Trade maps are converted into Trade records for performance.
        "
        [pair :- ValidPairs
         & [http-options :- HTTPOptionsValidation]]
        (public-api-call pair "trades" http-options))

(s/defn ^:always-validate get-depth
        "Get the public depth information for the supplied pair.

        Parameters:
        - pair (required): Must be a valid pair (see validation.clj for list)
        - http-options (optional): specify parameters such as keepalive, user-agent"
        [pair :- ValidPairs
         & [http-options :- HTTPOptionsValidation]]
        (public-api-call pair "depth" http-options))

(s/defn ^:always-validate get-fee
        "Get the public fee information for the supplied pair.

        Parameters:
        - pair (required): Must be a valid pair (see validation.clj for list)
        - http-options (optional): specify parameters such as keepalive, user-agent"
        [pair :- ValidPairs
         & [http-options :- HTTPOptionsValidation]]
        (public-api-call pair "fee" http-options))

; Trading API Functions

(defn get-info [account & [http-options]] (trade-api-call account {:method "getInfo"} http-options))

(s/defn ^:always-validate create-order
        "Create a buy/sell order.

        Parameters:
        - account (required): This is a map containing :api-key and :api-secret keys
        - order (required): This is a map containing :pair, :type, :rate and :amount keys
        - http-options (optional): Specify custom keepalive, user-agent"
        [account :- AccountValidation
         order :- OrderValidation
         & [http-options :- HTTPOptionsValidation]]
        (trade-api-call account (merge order {:method "Trade"}) http-options))

(s/defn ^:always-validate cancel-order
        "Cancel an order.

        Parameters:
        - account (required): This is a map containing :api-key and :api-secret keys
        - order_id (required): This is the order id to cancel
        - http-options (optional): Specify custom keepalive, user-agent"
        [account :- AccountValidation
         order_id :- Long
         & [http-options :- HTTPOptionsValidation]]
        (trade-api-call account {:order_id order_id :method "CancelOrder"} http-options))

(s/defn ^:always-validate get-trade-history
        "Retrieve completed trades history."
        [{:keys [account history-filter http-options] :as params} :- HistoryValidation]
        (trade-api-call account (merge history-filter {:method "TradeHistory"}) http-options))

(s/defn ^:always-validate get-transaction-history
        "Retrieve transaction history."
        [{:keys [account history-filter http-options] :as params} :- HistoryValidation]
        (trade-api-call account (merge history-filter {:method "TransHistory"}) http-options))

(s/defn get-active-orders
        "Retrieves open orders for currency pair."
        [account :- AccountValidation
         pair :- ValidPairs
         & [http-options :- HTTPOptionsValidation]]
        (trade-api-call account {:pair pair :method "ActiveOrders"} http-options))
