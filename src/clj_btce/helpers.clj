; Copyright (C) 2014 John. P Hackworth
;
; This Source Code Form is subject to the terms of the Mozilla Public
; License, v. 2.0. If a copy of the MPL was not distributed with this
; file, You can obtain one at http://mozilla.org/MPL/2.0/.

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Warning: These are experimental! ;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns clj-btce.helpers
 (:require [clj-btce.currencies :refer :all]
           [medley.core :refer [map-keys]])
 (:import [java.io File IOException FileNotFoundException]
           [java.nio.file Files Path LinkOption]
           [java.nio.file.attribute PosixFilePermissions PosixFileAttributes]))

(defn price-range 
  "This function is used to create a collection of evenly incremented 
  prices between start-price and end-price. 
  
  It can be useful for creating bulk-orders. For example:
  
  (let [start-price 0.5 
        end-price 0.6 
        units 10 
        template (map->Order {:currency1 :ltc :currency2 :btc :type \"sell\" :amount 1})]
    (map #(@(create-order (assoc template :rate %))) (price-range start-price end-price units))   
  
  --> THIS IS EXPERIMENTAL AND NOT FULLY TESTED. YOU HAVE BEEN WARNED. <--                                                                            
  "
  [^clj_btce.currencies.Currency currency 
   ^java.math.BigDecimal start-price 
   ^java.math.BigDecimal end-price 
   ^Integer units]
  (if (and (instance? clj_btce.currencies.Currency currency)
           (number? start-price)
           (number? end-price)
           (number? units))
    (let [diff (BigDecimal. (- end-price start-price))
        step (with-precision 10 (/ diff units))]
      (map #(currency %) (range start-price end-price step)))
     
    (-> "Invalid arguments supplied"
        (format)
        (Exception.)
        (throw))))

(def no-follow-links
  (into-array [LinkOption/NOFOLLOW_LINKS]))

(defn permissions [file]
  (try
    (-> (.toPath file)
        (Files/getPosixFilePermissions no-follow-links)
        PosixFilePermissions/toString)
    (catch UnsupportedOperationException e
      "")))

(defn world-readable? [path] 
  (let [perms (permissions (File. path))]
    (= \r (->> perms 
               (take-last 3)
               (first)))))

(defn world-writeable? [path] 
  (let [perms (permissions (File. path))]
    (= \w (->> perms 
               (take-last 3)
               (first)))))

(defn insecure-config? [path]
  (or (world-readable? path) (world-writeable? path)))
