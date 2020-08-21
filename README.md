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

#### client-for \[urls timeout\]
You will need a client object as the first argument to all the functions in this namespace.
This function constructs one given a collection of group <urls>, and a timeout (in seconds - defaults to 5).
If you haven't setup your own beacons, you can always use the LOE (League-Of-Entropy) ones (this is what the no-arg arity does). 

```clj
(def loe-client (drand/client-for)) 
```
You now have a client, and can start interacting with the `drand` beacons.
Simply creating the client has verified that the urls can be reached, and that the hashes match. 

#### get-info \[client\]
Queries the `/info` endpoint of each beacon returning the first result.

```clj
@(drand/get-info loe-client) ;; => map with keys ["public_key" "period" "genesis_time" "hash" "groupHash"]
```

#### get-public \[client round\]
Queries the `/public/<round>` endpoint of each beacon returning the first result. `round` defaults to the latest round.

```clj
@(drand/get-public loe-client) ;; => map with keys ["round" "randomness" "signature" "previous_signature"]
```

#### get-entropy \[client round\]
Builds on top of `get-public`, extracting the hex-encoded randomness, and decoding it. `round` defaults to the latest round.
Unlike most of the functions in this namespace, this is synchronous.

```clj
(drand/get-entropy loe-client) ;; => byte-array (32 elements)
```


#### round-at \[client instant\]
Returns the round of generated randomness at the given <instant> (`java.time.Instant`).

```clj
(drand/round-at loe-client (Instant/now)) ;; => a positive integer 
```

#### entropy-watch \[client watch-fn\]
Schedules periodic consumption of entropy (via `watch-fn`). Returns a no-arg fn to un-schedule.
Consumption does NOT start immediately, but on the next refresh, and every 'period' (see `get-info`) seconds. 

```clj
(def unwatch!
  (drand/entropy-watch loe-client #(println (ZonedDateTime/now) ":" (seq %))))
;; you should start seeing print-outs shortly (less than 30 seconds) 

(unwatch!) ;; => true
;; there should be no more print-outs

```

#### with-caching \[client api-fn\]
Memoizes <api-fn> in TTL (time-to-live) fashion.
The returned function will block on the very first call (waiting for the next refresh), 
and start refreshing its cache every 'period' (see `get-info`) seconds thereafter.

If it's not quite obvious why this might be useful, consider the following situation:
You want to consume entropy, but you want to be the one who drives it (i.e. `entropy-watch` won't cut it).

```clj
(def cached-entropy
  (drand/with-caching loe-client drand/get-entropy))

(def process-entropy
  (comp #(println (ZonedDateTime/now) ":" (seq %)) ;; same consumer-fn we used in `entropy-watch`
        cached-entropy))

(process-entropy) ;; call it once to calculate when the next-refresh will be and block until then
```
You can now call `process-entropy` as frequently as you want (http-calls will only be made every 30 seconds) -
understanding of course that most of those calls will return identical values (depending on frequency).

#### with-http-client \[http-client & body\]
This is just a convenience macro for overriding the default http-client (constructed with `(HttpClient/newHttpClient)`).

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
