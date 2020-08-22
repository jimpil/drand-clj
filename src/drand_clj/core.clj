(ns drand-clj.core
  (:require [drand-clj
             [client :as client]
             [impl   :as impl]
             [urls   :as urls]]
            [clojure.core.memoize :as memoize]))

(defonce loe-group ;; league-of-entropy
  [urls/cloudflare
   urls/protocol-labs1
   urls/protocol-labs2
   urls/protocol-labs3])

(defn client-for
  "Creates a new `drand` (http) client with the
   given group <urls> and <timeout> (in seconds).
   The returned object is to be passed as the first
   argument to all functions in this namespace."
  ([]
   (client-for loe-group))
  ([urls]
   (client-for urls 5))
  ([urls timeout]
   (client/wrap urls timeout)))

(defn get-info
  "Queries the /info api of this client's group.
   Returns a promise."
  [drand-client]
  (impl/info drand-client))

(defn get-public
  "Queries the /public/<round> api of this client's group.
   Returns a promise. You typically don't need to call this
   as the info never changes, and has already been fetched -
   use `(:group-info drand-client)` instead."
  ([drand-client]
   (get-public drand-client 0))
  ([drand-client round]
   (impl/getPublicRound drand-client round)))

(defn get-entropy
  "Returns 32 bytes (byte-array) of `drand` randomness.
   Will return the same value if called before the group
   refreshes its randomness."
  (^bytes [drand-client]
   (get-entropy drand-client 0))
  (^bytes [drand-client round]
   (impl/entropyAt drand-client round)))

(defn entropy-watch
  "Schedules periodic execution of <watch-fn!> against fresh entropy
   as soon as it is produced (starting at 'the next available time').
   Returns a no-arg fn to un-schedule."
  [drand-client watch-fn!]
  (impl/entropyWatch drand-client watch-fn!))

(defn round-at
  "Returns the round (a positive integer) at
  <instant> (a `java.time.Instant`)."
  [drand-client instant]
  (impl/roundAt drand-client instant))


(defn with-caching
  "Returns a TTL (time-to-live) cached version of `(partial <f> <drand-client>)`,
   with a :ttl/threshold equal to the client's period (calculated from its info).
   The very first call will block until the 'next refresh', which could be up to
   `(dec period)` seconds. After that, there is no delay.

   Example use-case:
   Define a synchronous/caller-driven version of `entropy-watch`

   (def get-entropy-cached
     (with-caching <the-client> get-entropy))

   (def process-entropy
     (comp consume! ;; the same callback you would pass to `entropy-watch`
           get-entropy-cached))

    ;; the following will delay/block on the first call
    ;; just like `entropy-watch` schedules with an initial delay
    (process-entropy)"
  [drand-client f]
  (let [{:strs [genesis_time period]} (:group-info drand-client)
        delay? (volatile! true)
        delay-wrapped (fn [& args]
                        (when @delay?
                          (let [dlay (impl/next-round-in genesis_time period)]
                            (vswap! delay? false)
                            (Thread/sleep (* 1000 dlay))))
                        (apply f drand-client args))]
    (memoize/ttl delay-wrapped :ttl/threshold (* 1000 period))))

(defmacro with-http-client
  "Helper macro for overriding the default http-client."
  [http-client & body]
  `(binding [impl/*http-client* ~http-client]
     ~@body))

(comment
  (def client (client-for))
  ;; use the client
  @(get-info client) ;; same as `(:group-info client)`
  @(get-public client 51)
  @(get-public client)
  (round-at client (Instant/now))
  (def unwatch!
    (entropy-watch client #(println (ZonedDateTime/now) ":" (seq %))))
  (unwatch!) ;; => true

  (def cached-entropy
    (with-caching client get-entropy))

  (def process-entropy
    (comp #(println (ZonedDateTime/now) ":" (seq %))
          cached-entropy))

  )
