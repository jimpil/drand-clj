# drand-clj

## What
A Clojure client for the [drand](https://drand.love/) HTTP API.

## Where

todo

## Usage

### drand-clj.core
This is the top-level API for `drand-clj`. It is asynchronous all the way down, so _most_ functions return a `Promise`.

```clj
(require '[drand-clj.core :as drand]) ;; first things first
```

#### _client-for_ \[urls timeout\]
You will need a client object as the first argument to all the functions in this namespace.
This function constructs one given a collection of group _urls_, and a _timeout_ (in seconds - defaults to 5).
If you haven't setup your own beacons, you can always use the **LOE** (_League-Of-Entropy_) ones 
(this is what the no-arg arity does). 

```clj
(def loe-client (drand/client-for)) 
```
You now have a client, and can start interacting with the `drand` beacons.
Simply creating the client has verified that the urls can be reached, and that the hashes match. 

#### _get-info_ \[client\]
Queries the `/info` endpoint of each beacon returning the fastest response. The same result can be obtained via 
`(:group-info client)`.

```clj
@(drand/get-info loe-client) ;; => map with keys ["public_key" "period" "genesis_time" "hash" "groupHash"]
```

#### _get-public_ \[client round\]
Queries the `/public/<round>` endpoint of each beacon returning the fastest response. _round_ defaults to the latest round.

```clj
@(drand/get-public loe-client) ;; => map with keys ["round" "randomness" "signature" "previous_signature"]
```

#### _get-entropy_ \[client round\]
Builds on top of `get-public`, extracting the hex-encoded randomness, and decoding it. _round_ defaults to the latest round.
Unlike most of the functions in this namespace, this returns a byte-array of 32 elements (not a `Promise`).

```clj
(drand/get-entropy loe-client) ;; => byte-array
```

#### _round-at_ \[client instant\]
Returns the round of generated randomness at the given _instant_ (`java.time.Instant`).

```clj
(drand/round-at loe-client (Instant/now)) ;; => a positive integer 
```

#### _entropy-watch_ \[client watch-fn\]
Schedules periodic consumption of entropy (via _watch-fn_). Returns a no-arg fn to un-schedule.
Consumption does NOT start immediately, but on the next refresh, and every 'period' (see `get-info`) seconds. 

```clj
(def unwatch!
  (drand/entropy-watch loe-client #(println (ZonedDateTime/now) ":" (seq %))))
;; you should start seeing print-outs shortly (less than 30 seconds) 
;; visually confirm correct synchronization against the UI at https://drand.love/ 

(unwatch!) ;; => true
;; there should be no more print-outs

```

#### _with-ttl-caching_ \[client api-fn\]
Memoizes _api-fn_ in a TTL (time-to-live) fashion.
The returned function will block on the very first call (waiting for the next refresh), 
and start refreshing its cache every 'period' (see `get-info`) seconds thereafter.

If it's not quite obvious why this might be useful, consider the following situation:
You want to consume entropy, but you want to be the one who drives it (i.e. `entropy-watch` won't cut it).

```clj
(def cached-entropy
  (drand/with-caching loe-client drand/get-entropy))

(def process-entropy
  (comp #(println (ZonedDateTime/now) ":" (seq %)) ;; same consumer-fn used in `entropy-watch`
        cached-entropy))

(process-entropy) ;; call it once to calculate when the next-refresh will be and block until then
```
You can now call `process-entropy` as frequently as you want (http-calls will only be made every 'period' seconds) -
understanding of course that those calls will return identical values (depending on frequency).

#### _with-http-client_ \[http-client & body\]
Convenience macro for overriding the default _http-client_ (i.e. `(HttpClient/newHttpClient)`).

#### _strong-random_ \[entropy\]
Returns the strongest possible instance of `SecureRandom` (on the running platform) 
with 32 bytes of added _entropy_ (supplementing its own seed).

```clj
(drand/strong-random (drand/get-entropy loe-client)) ;; => #object[java.security.SecureRandom 0x680f108e "Blocking"]
```

### drand-clj.client
If for some reason you want a lower-level API that doesn't require a client object,
this namespace might be useful. Functions like `get-info*`, `get-public*` and `get-public-round*`
expect named arguments `:url`/`:timeout-seconds` (defaulting to `Cloudflare`/`5`). 
Bypassing the client object like this, obviously means no group-info validation 
(there is no group at this level - just a URL), but also no `core.async` involvement 
(the promise will be delivered straight from the async http-handler). 
I guess these functions are the quickest/easiest way of testing/debugging/trying out individual urls.

## Requirements
- Some recent version of Java (>= 11)

## License

Copyright © 2020 Dimitrios Piliouras

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
