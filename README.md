# clj-btce

API Library for BTC-E

## Changes from 0.1.0 to 0.2.0

- Major refactoring
- Functions now return promises instead of blocking for result.
- Function input validation using prismatic/schema 
- Added experimental currency support 
- Added helper functions (price-range) 

## Changes from 0.0.2 to 0.1.0

### Libraries

- Removed a lot of libraries that were left in 0.0.2 for testing. Startup should be much faster
- Using [Cheshire](https://github.com/dakrone/cheshire) for json parsing

### Configuration

- Configuration is now centrally stored in @config map 
- Credentials configured the same as in 0.0.2
- New @config keys include :keepalive (in ms), :insecure (allow insecure http request), :trade-api-url, :public-api-url, :user-agent

### Trade api calls 

- Data previously in :return key, now returned at top-level. In other words: (:return (btce/get-info)) in 0.0.2 is now (btce/get-info) in 0.1.x.

### Error handling

- Exceptions are raised if :api-key or :api-secret is not set, and a trade api call is made
- If HTTP response is not 200, an exception is raised with the unparsed HTTP response 
- If HTTP response code is 200 and there is an error, an exception will be raised with the parsed response body as a map.

## Installation

### In Repl

    user=> (use `clj-btce.core)

### As dependency in project.clj

    [clj-btce "0.2.0"]

### Require in namespace

    (:require [clj-btce.core :as btce])    

## Configuration

### Trade API Credentials

All Trade API calls require a map containing :api-secret and api-keys.

```clojure
(def account {:api-key "abcd..." :api-secret "abcd..."})
```

### HTTP Options

All requests allow you to specify alternative (http-kit) client [HTTP Options](http://http-kit.org/client.html#options)

Important http options include keepalive, insecure?, user-agent.

NOTE: This feature is included for convenience but hasn't been fully tested. Use with caution.

**Example**:

```clojure
user=> (def http-options {:keepalive 600 :user-agent "MyApp 1.0"})
@'user/http-options
user=> (def account {:api-key "abcd" :api-secret "abcd"})
#'user/account
user=> (def order {:pair "ltc_btc" :type "sell" :rate 5 :amount 1})
#'user/order
user=> @(create-order account order http-options)
{:success 1, :return {:received 0, :remains 1, :order_id 182510000, :funds {:trc 0, :eur 0, :ppc 0, :rur 0, :ltc 1, :ftc 0, :btc 0, :usd 0, :nmc 0, :xpm 0, :nvc 0}}}
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
user=> @(get-fee "btc_usd")
{:trade 0.2}

user=> @(get-ticker "ltc_btc")
#clj_btce.core.Ticker{:last 0.02749, :vol_cur 60328.92732, :low 0.02704, :updated 1395563126, :buy 0.02757, :vol 1654.38736, :sell 0.02748, :avg 0.02743, :server_time 1395563126, :high 0.02782}

user=> (def nmc-btc-trades @(get-trades "nmc_btc"))
#'user/nmc-btc-trades
user=> (count nmc-btc-trades)
150
user=> (first nmc-btc-trades)
#clj_btce.core.Trade{:date 1395562477, :price 0.0048, :amount 0.8, :tid 33127844, :price_currency "BTC", :item "NMC", :trade_type "bid"}

user=> (def ppc-btc-depth @(get-depth "ppc_btc"))
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

_(get-info [account & [http-options]])_

```clojure
user=> (def account {:api-key "abcd" :api-secret "abcd"})
#'user/account
user=> @(get-info account)
{:success 1, :return {:funds {:trc 0, :eur 0, :ppc 9, :rur 0, :ltc 2, :ftc 0, :btc 0, :usd 0, :nmc 0, :xpm 0, :nvc 0}, :rights {:info 1, :trade 1, :withdraw 0}, :transaction_count 2000, :open_orders 4, :server_time 1395500000}}
```

#### Create a buy/sell order

_(create-order [account order & [http-options]])_

```clojure
user=> (def account {:api-key "abcd" :api-secret "abcd"})
#'user/account
user=> (def order {:pair "ltc_btc" :type "sell" :rate 5 :amount 1})
#'user/order
user=> @(create-order account order)
{:success 1, :return {:received 0, :remains 1, :order_id 182510000, :funds {:trc 0, :eur 0, :ppc 0, :rur 0, :ltc 1, :ftc 0, :btc 0, :usd 0, :nmc 0, :xpm 0, :nvc 0}}}
```

#### Cancel an existing order

_(cancel-order [account orderid & [http-options]])_

```clojure
user=> (def account {:api-key "abcd" :api-secret "abcd"})
#'user/account
user=> @(cancel-order account 182510000)
{:success 1, :return {:received 0, :remains 1, :order_id 182510000, :funds {:trc 0, :eur 0, :ppc 0, :rur 0, :ltc 2, :ftc 0, :btc 0, :usd 0, :nmc 0, :xpm 0, :nvc 0}}}
```

#### Get active orders for currency pair

_(get-active-orders [account pair & [http-options])_

```clojure
user=> (def account {:api-key "abcd" :api-secret "abcd"})
#'user/account
user=> @(get-active-orders acct "nmc_btc")
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
                               :end <unix time to finish displaying from>}
                      :http-options}]])
```

**Example**:

```clojure
user=> (def account {:api-key "abcd" :api-secret "abcd"})
#'user/account
user=> @(get-trade-history {:account account :history-filter {:pair "ltc_btc" :count 1}})
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
                               :end <unix time to finish displaying from>}
                      :http-options}]])
```

**Example**:

```clojure
user=> (def account {:api-key "abcd" :api-secret "abcd"})
#'user/account
user=> @(get-transaction-history {:account account :history-filter {:count 1}})
{:success 1, :return {:418700000 {:type 4, :amount 1.0, :currency "NMC", :desc "Cancel order :order:177670000:", :status 2, :timestamp 1395560000}}}
```

### Todo 

- Implement queueing for API requests to manage nonce limitations
- Increase test coverage

### Bugs

- Due to current nonce implementation, if you make several requests simultaneously, you may have failures. For best success, try to limit to 4/req per second.
- No testing implemented. 
- Configuration subject to change
- API subject to change

You should not depend on this for production.

## License

Copyright © 2014 John P. Hackworth <jph@hackworth.be>

Distributed under the Mozilla Public License Version 2.0