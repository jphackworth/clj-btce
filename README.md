# clj-btce

API Library for BTC-E

## Installation

### In Repl

    user=> (use `clj-btce.core)

### As dependency in project.clj

    [clj-btce "0.0.2"]

### Require in namespace

    (:require [clj-btce.core :as btce])    

## Configuring API Credentials

    user=> (configure :api-key "blah" :api-secret "blah")
    {:api-key "blah", :api-secret "blah"}
    user=> @credentials
    {:api-key "blah", :api-secret "blah"}

## Usage

### Public API

- (get-fee [currency1 currency2])
- (get-ticker [currency1 currency2])
- (get-trades [currency1 currency2])
- (get-depth [currency1 currency2])

#### Public API Examples

    (get-fee :btc :usd)
    (get-ticker :ltc :btc)
    (get-trades :nmc :btc)
    (get-depth :ppc :usd)

### Trading API

See [BTC-E Trading API](https://btc-e.com/api/documentation) for official API documentation.

- (get-info)
- (get-transaction-history :from <from> :limit <count> :from_id <from_id> :end_id <end_id> :since <since> :end <end>)
- (get-trade-history :from <from> :limit <count> :from_id <from_id> :end_id <end_id> :since <since> :end <end> :pair <pair>)
- (get-active-orders <currency pair>)
- (create-trade [currency1 currency2 type rate amount])
- (cancel-order [orderid])

#### Trading API Examples

    (get-info)
    (get-transaction-history) ; keyword arguments are optional
    (get-trade-history) ; keyword arguments are optional
    (get-trade-history :limit 20)
    (get-active-orders) ; arguments are optional
    (get-active-orders "nmc_btc")
    (create-trade :nmc :btc "buy" 0.001 1)
    (cancel-order 12345)



...

### Bugs

- No testing implemented. 
- Configuration subject to change
- API subject to change
- No support for Fee / Profit calculations

You should not use this in production.

## License

Copyright © 2013 John P. Hackworth <jph@hackworth.be>

Distributed under the Mozilla Public License Version 2.0