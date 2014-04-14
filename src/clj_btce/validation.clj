(ns clj-btce.validation
  (:require [schema.core :as s]))

(def ValidPairs (s/enum "btc_usd" "btc_rur" "btc_eur" "btc_cnh" "btc_gbp" 
                        "ltc_btc" "ltc_usd" "ltc_rur" "ltc_eur" "ltc_cnh" "ltc_gbp" 
                        "nmc_btc" "nmc_usd" 
                        "nvc_btc" "nvc_usd" 
                        "usd_rur" "eur_usd" "eur_rur" "usd_cnh" "gbp_usd"
                        "trc_btc" 
                        "ppc_btc" "ppc_usd" "ftc_btc" "xpm_btc"))
(def ValidOrderMethods (s/enum "Trade" "CancelTrade"))
(def AccountValidation {(s/required-key :api-key) s/Str 
                        (s/required-key :api-secret) s/Str})
(def OrderValidation {(s/optional-key :method) ValidOrderMethods
                      (s/required-key :pair) ValidPairs
                      (s/required-key :type) (s/enum "buy" "sell")
                      (s/required-key :rate) Number 
                      (s/required-key :amount) Number})

;{:date 1397482275, :price 452.798, :amount 5, :tid 35428347, :price_currency "USD", :item "BTC", :trade_type "bid"}

(def TradesValidation {(s/required-key :date) Number
                       (s/required-key :price) Number 
                       (s/required-key :amount) Number 
                       (s/required-key :tid) Number 
                       (s/required-key :price_currency) s/Str 
                       (s/required-key :item) s/Str 
                       (s/required-key :trade_type) (s/enum "bid" "ask")})     
                  

(defrecord History [method from count from_id end_id order since end success return error http-options])

(def HistoryFilterValidation {(s/optional-key :method) (s/enum "TradeHistory" "TransHistory")
                              (s/optional-key :from) Number 
                              (s/optional-key :count) Long
                              (s/optional-key :from_id) Long 
                              (s/optional-key :end_id) Long 
                              (s/optional-key :order) (s/enum "ASC" "DESC") 
                              (s/optional-key :since) Long 
                              (s/optional-key :end) Long 
                              (s/optional-key :pair) ValidPairs})

(def HistoryValidation {(s/required-key :account) AccountValidation 
                         (s/optional-key :history-filter) HistoryFilterValidation})