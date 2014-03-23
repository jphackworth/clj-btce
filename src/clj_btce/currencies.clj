; Warning - these are experimental

(ns clj-btce.currencies)

(defn round$ [c amount]
  (let [decimal-points (get c :decimal-points)
        d (bigdec amount)]
    (if (= decimal-points (.scale d))
      d
      (.setScale d decimal-points java.math.RoundingMode/HALF_UP))))

(defn money-formatter [c]
  (let [ decimal-points (:decimal-points c)
         fs (if (= 0 decimal-points) "#,###,###" (str "#,###,###." (reduce str (repeat decimal-points "#"))))]
      (java.text.DecimalFormat. fs)
    ))

(defn parse$ [c value]
  (round$ c (.parse (money-formatter c) (first (re-find #"([0123456789.,]+)" value )))))

(defrecord Currency 
  [name iso-code symbol decimal-points]
  clojure.lang.IFn
  (invoke [this] (this 0))  
  (invoke [this value] 
    (if (instance? String value)
      (parse$ this value)
      (round$ this value)))
  (applyTo [this args] (clojure.lang.AFn/applyToHelper this args)))

(def RUB (map->Currency {:symbol "р." :decimal-points 2 :name "Russian Ruble" :iso-code "RUB" })) 
(def EUR (map->Currency {:symbol "€" :decimal-points 2 :name "Euro", :iso-code "EUR"})) 
(def USD (map->Currency {:symbol "$" :decimal-points 2 :name "United States Dollar", :iso-code "USD"})) 
(def BTC (map->Currency {:symbol "฿" :decimal-points 8 :name "Bitcoin" :iso-code "BTC"}))
(def LTC (map->Currency {:symbol nil :decimal-points 8 :name "Litecoin" :iso-code "LTC"}))
(def NMC (map->Currency {:symbol nil :decimal-points 8 :name "Namecoin" :iso-code "NMC"}))
(def NVC (map->Currency {:symbol nil :decimal-points 8 :name "Novacoin" :iso-code "NVC"}))
(def TRC (map->Currency {:symbol nil :decimal-points 8 :name "Terracoin" :iso-code "TRC"}))
(def PPC (map->Currency {:symbol nil :decimal-points 8 :name "Peercoin" :iso-code "PPC"}))
(def DGC (map->Currency {:symbol nil :decimal-points 8 :name "Dogecoin" :iso-code "DGC"}))