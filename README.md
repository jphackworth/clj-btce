# clj-btce

API Library for BTC-E

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

    [clj-btce "0.1.0"]

### Require in namespace

    (:require [clj-btce.core :as btce])    

## Configuring API Credentials

In REPL:

    user=> (configure :api-key "blah" :api-secret "blah")
    {:api-key "blah", :api-secret "blah"}
    user=> @config
    {:api-key "blah", :api-secret "blah" :insecure false, :public-api-url "https://btc-e.com/api/2", :user-agent "clj-btce 0.1.0", :keepalive 30000, :trade-api-url "https://btc-e.com/tapi" :nonce-ms 500}

In code:

    (btce/configure :api-key "blah" :api-secret "blah")

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
- (get-transaction-history :from from :limit count :from_id from_id :end_id end_id :since since :end end)
- (get-trade-history :from <from> :limit count :from_id from_id :end_id end_id :since since :end end :pair pair)
- (get-active-orders "btc_usd")
- (create-trade [currency1 currency2 type rate amount])
- (cancel-order [orderid])

#### Trading API Examples

    (get-info) ; balance information
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

Copyright © 2014 John P. Hackworth <jph@hackworth.be>

Distributed under the Mozilla Public License Version 2.0