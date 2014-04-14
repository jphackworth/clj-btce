(ns clj-btce.core-test
  (:require [clojure.test :refer :all]
            [clojure.edn :as edn]
            [clj-btce.validation :refer :all]
            [schema.core :as s]
            schema.test
            [clj-btce.core :refer :all]))

(use-fixtures :once schema.test/validate-schemas)

(defn load-config [& [filename]] 
  (let [filename (if-not (nil? filename) 
                   filename
                   (str (System/getenv "HOME") "/.btce.conf"))]
      (edn/read-string (slurp filename))))

;;; Public API Testing - No credentials required

(deftest test-ticker 
  (testing "Getting btc_usd ticker" 
    (is (> (:updated (:ticker (get-ticker "btc_usd"))) 1))))

(deftest test-trades 
  (testing "Getting btc_usd trades" 
    (s/validate TradesValidation (first (get-trades "btc_usd"))))) 

(deftest test-depth 
  (testing "Getting btc_usd depth" 
    (is (= '(:asks :bids) (keys (get-depth "btc_usd"))))))

(deftest test-fee 
  (testing "Getting btc_usd fee" 
    (s/validate s/Num (:trade (get-fee "btc_usd")))))

;;; Trade API Testing - Information-related

(def account (load-config))
(s/validate AccountValidation account)

(deftest test-getinfo 
  (testing "Getting account info" 
    (is (= 1 (:success (get-info account))))))

(deftest test-trade-history 
  (testing "Getting basic trade history" 
    (is (= 1 (:success (get-trade-history {:account account}))))))

(deftest test-trans-history 
  (testing "Getting default transaction history" 
    (is (= 1 (:success (get-transaction-history {:account account}))))))

;;; Trading API Testing - Trade related

; (def order-id (atom nil))
; (def test-sell-order {:pair "ltc_btc"
;                       :type "sell"
;                       :rate 1 ; BTC value 
;                       :amount 1}) ; units

; (s/validate OrderValidation test-sell-order)

; (deftest test-create-order
;   (testing "Create sell order for 1 ltc at 1 BTC"
;     (is (= 1 (let [{:keys [success return] :as response} (create-order account test-sell-order)]
;                (if (= success 1)
;                  (reset! order-id (:order_id return)))
;                success)))))

; (deftest test-active-orders 
;   (testing "Getting active orders" 
;     (is (= 1 (:success (get-active-orders account "ltc_btc"))))))

; (deftest test-cancel-order 
;   (testing "Canceling previous order"
;     (if-not (nil? @order-id)
;       (is (= 1 (:success (cancel-order account @order-id))))
;       false)))


   