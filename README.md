# clj-btce

API Library for BTC-E

[clj-btce "0.3.0"]

## Major/Breaking changes in 0.3.x since 0.2.x 

- All requests are synchronous. Async temporarily removed
- Trade function (optional) parameters have changed
- Returned ticker/trades are plain arraymap values (no longer use Trade/Ticker records) 
- Replaced http-kit with clj-http
- Custom nonces are possible if you wish to manage nonce generation yourself
- Mandatory parameter validation only enabled on create-order and cancel-order
- Basic tests for creating and cancelling orders. Note: these require credentials and will create and cancel a **real** order.

## Installation

### In Repl

    user=> (use `clj-btce.core)

### As dependency in project.clj

    [clj-btce "0.3.0"]

### Require in namespace

    (:require [clj-btce.core :as btce])    

## Configuration

### Trade API Credentials

All Trade API calls require a map containing :api-secret and api-keys.

```clojure
(def account {:api-key "abcd..." :api-secret "abcd..."})
```

### Trade Request Options

All trade requests accept an optional parameter containing the following keys:

- nonce: A custom nonce value
- http-options: custom ([clj-http](https://github.com/dakrone/clj-http)) client options. Examples include socket-timeout and conn-timeout.

Example:

```clojure
(def account {:api-secret "abcd" :api-key "abcd"})
(get-info account {:nonce 100 
                   :http-options {:socket-timeout 100}})
```




### API URLs

Both the public and trading API urls are atomic variables. If there is ever a reason to change them, here is how to do it:

```clojure
user=> (use 'clj-btce.core)
nil
user=> @trade-api-url
"https://btc-e.com/tapi"
user=> @public-api-url
"https://btc-e.com/api/2"

user=> (reset! trade-api-url "http://somewhere-else.com")
"http://somewhere-else.com"
user=> (reset! public-api-url "http://somewhere-else.com")
"http://somewhere-else.com"

user=> @trade-api-url
"http://somewhere-else.com"
user=> @public-api-url
"http://somewhere-else.com"
```

## Library Usage

### Public API

```clojure
(get-fee [pair & [http-options]]) ; get fee for trading pair
(get-ticker [pair & [http-options]]) ; get latest ticker for pair
(get-trades [pair & [http-options]]) ; get list of latest trades for pair
(get-depth [pair & [http-options]]) ; get list of open orders for current pair
```

**Examples**:

```clojure
user=> (get-fee "btc_usd")
{:trade 0.2}

user=> (get-ticker "ltc_btc")
{:ticker {:last 0.02749, :vol_cur 60328.92732, :low 0.02704, :updated 1395563126, :buy 0.02757, :vol 1654.38736, :sell 0.02748, :avg 0.02743, :server_time 1395563126, :high 0.02782}

user=> (def nmc-btc-trades (get-trades "nmc_btc"))
#'user/nmc-btc-trades
user=> (count nmc-btc-trades)
150
user=> (first nmc-btc-trades)
{:date 1395562477, :price 0.0048, :amount 0.8, :tid 33127844, :price_currency "BTC", :item "NMC", :trade_type "bid"}

user=> (def ppc-btc-depth (get-depth "ppc_btc"))
#'user/ppc-btc-depth
user=> (keys ppc-btc-depth)
(:asks :bids)
user=> (count (:asks ppc-btc-depth))
150
user=> (count (:bids ppc-btc-depth))
150
user=> (first (:asks ppc-btc-depth))
[0.00469 15.59111481]
user=> (first (:bids ppc-btc-depth))
[0.00466 26.84995544]
```

### Trading API

See [BTC-E Trading API](https://btc-e.com/api/documentation) for official API documentation.

#### Get Info (Funds)

Returns information about your available funds on btc-e.

_(get-info [account & [{:nonce <custom nonce> :http-options {<custom clj-http options>}}]])_

```clojure
user=> (def account {:api-key "abcd" :api-secret "abcd"})
#'user/account
user=> (get-info account)
{:success 1, :return {:funds {:trc 0, :eur 0, :ppc 9, :rur 0, :ltc 2, :ftc 0, :btc 0, :usd 0, :nmc 0, :xpm 0, :nvc 0}, :rights {:info 1, :trade 1, :withdraw 0}, :transaction_count 2000, :open_orders 4, :server_time 1395500000}}
```

#### Create a buy/sell order

_(create-order [account order & [{:nonce <custom nonce> :http-options {.. }}]])_

```clojure
user=> (def account {:api-key "abcd" :api-secret "abcd"})
#'user/account
user=> (def order {:pair "ltc_btc" :type "sell" :rate 5 :amount 1})
#'user/order
user=> (create-order account order)
{:success 1, :return {:received 0, :remains 1, :order_id 182510000, :funds {:trc 0, :eur 0, :ppc 0, :rur 0, :ltc 1, :ftc 0, :btc 0, :usd 0, :nmc 0, :xpm 0, :nvc 0}}}
```

#### Cancel an existing order

_(cancel-order [account orderid & [http-options]])_

```clojure
user=> (def account {:api-key "abcd" :api-secret "abcd"})
#'user/account
user=> (cancel-order account 182510000)
{:success 1, :return {:received 0, :remains 1, :order_id 182510000, :funds {:trc 0, :eur 0, :ppc 0, :rur 0, :ltc 2, :ftc 0, :btc 0, :usd 0, :nmc 0, :xpm 0, :nvc 0}}}
```

#### Get active orders for currency pair

_(get-active-orders [account pair & [http-options])_

```clojure
user=> (def account {:api-key "abcd" :api-secret "abcd"})
#'user/account
user=> (get-active-orders account "nmc_btc")
{:success 1, :return {:18252000 {:pair "nmc_btc", :type "sell", :amount 1.0, :rate 0.9, :timestamp_created 1395000001, :status 0}}}
```

#### Get Completed Trades History

```clojure
(get-trade-history [{:account 
                     :history-filter {:pair <currency pair, "btc_usd"> 
                                      :from <transaction id> 
                                      :count <number of transactions to return> 
                                      :from_id <transaction id to start with> 
                                      :end_id <transaction id to end with> 
                                      :order <sort ASCending or DESCending> 
                                      :since <unix time to display from> 
                                      :end <unix time to finish displaying from>}}
                     {:nonce <custom nonce> 
                      :http-options {:socket-timeout 2000}}]])
```

**Example**:

```clojure
user=> (def account {:api-key "abcd" :api-secret "abcd"})
#'user/account
user=> (get-trade-history {:account account :history-filter {:pair "ltc_btc" :count 1}})
{:success 1, :return {:32810000 {:pair "ltc_btc", :type "sell", :amount 1, :rate 0.08, :order_id 178450000, :is_your_order 1, :timestamp 1395210000}}}
```

#### Get Transaction History

```clojure
(get-transaction-history [{:account 
                           :filter {:pair <currency pair, "btc_usd"> 
                                    :from <transaction id> 
                                    :count <number of transactions to return> 
                                    :from_id <transaction id to start with> 
                                    :end_id <transaction id to end with> 
                                    :order <sort ASCending or DESCending> 
                                    :since <unix time to display from> 
                                    :end <unix time to finish displaying from>}}
                           {:nonce <custom nonce> 
                            :http-options {:socket-timeout 2000}]])
```

**Example**:

```clojure
user=> (def account {:api-key "abcd" :api-secret "abcd"})
#'user/account
user=> (get-transaction-history {:account account :history-filter {:count 1}})
{:success 1, :return {:418700000 {:type 4, :amount 1.0, :currency "NMC", :desc "Cancel order :order:177670000:", :status 2, :timestamp 1395560000}}}
```

### Todo 

- Increase test coverage

### Bugs

- No testing implemented. 
- Configuration subject to change
- API subject to change

You should not depend on this for production.

## License

Copyright © 2014 John P. Hackworth <jph@hackworth.be>

Distributed under the Mozilla Public License Version 2.0