(ns drand-clj.core
  (:require [drand-clj
             [client :as client]
             [impl   :as impl]
             [urls   :as urls]]))

(defonce loe-group ;; league of entropy
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
   Returns a promise."
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
  <instant> (an instance of `java.time.Instant`)."
  [drand-client instant]
  (impl/roundAt drand-client instant))

(defmacro with-http-client
  "Helper macro for overriding the default http-client."
  [http-client & body]
  `(binding [impl/*http-client* ~http-client]
     ~@body))

(comment
  (def client (client-for))
  ;; use the client
  @(get-info client)
  @(get-public client 51)
  @(get-public client)
  (round-at client (Instant/now))
  (def unwatch!
    (entropy-watch client #(println (ZonedDateTime/now) ":" (seq %))))
  (unwatch!) ;; => true

  )
