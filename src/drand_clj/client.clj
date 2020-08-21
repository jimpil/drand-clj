(ns drand-clj.client
  (:require [drand-clj.impl :as impl]
            [drand-clj.urls :as urls]
            [clojure.core.async :as async])
  (:import (java.util.concurrent TimeUnit Executors)
           (clojure.lang Var)))

(set! *warn-on-reflection* true)

(defn- async-handler
  [custom-handler]
  (let [ret-promise (when (nil? custom-handler) (promise))
        handler (or custom-handler (partial deliver ret-promise))]
    [ret-promise handler]))

;;INFO
(def ^:private default-info-request
  (-> urls/cloudflare
      (impl/info-request  5)
      delay))

(defn get-info
  "Top-level fn for retrieving `drand` beacon info."
  [& {:keys [url timeout-seconds http-request response-handler]}]
   (let [[ret-promise handler] (async-handler response-handler)]
     (impl/send-request!
       (or http-request
           (some-> url (impl/info-request timeout-seconds))
           @default-info-request)
       handler)
     ret-promise))

;; GET
(def ^:private default-public-request
  (-> urls/cloudflare
      (impl/public-latest-request 5)
      delay))

(defn get-public
  ""
  [& {:keys [url timeout-seconds http-request response-handler]}]
  (let [[ret-promise handler] (async-handler response-handler)
        request (or http-request
                    (some-> url (impl/public-latest-request timeout-seconds))
                    @default-public-request)]
    (impl/send-request! request handler)
    ret-promise))

(defn get-public-round
  ""
  [round & {:keys [url timeout-seconds response-handler]}]
  (let [[ret-promise handler] (async-handler response-handler)
        request (when (some-> round pos?) ;; covers nil?/zero?/neg?
                  (impl/request* (str url urls/public-round round) timeout-seconds))]
    (if request
      (do (impl/send-request! request handler)
          ret-promise)
      (get-public :url url
                  :timeout-seconds timeout-seconds
                  :response-handler response-handler))))

;;=================================================================

(defn- fastest-query
  "Asynchronously queries all <urls> (via <api-fn>),
   delivering the first (fastest) response into the promise returned.
   If <timeout-seconds> is exceeded returns `::timeout`."
  [urls timeout-seconds api-fn]
  (let [[ret-chan http-chan] (repeatedly 2 async/promise-chan)
        ret-promise (promise)
        timeout-ms (* 1000 timeout-seconds)]

    (doseq [url urls]
      (async/go
        (api-fn ;; non-blocking
          :url url
          :timeout-seconds (inc timeout-seconds) ;; don't want to see `::http-timeout` here
          :response-handler (partial async/put! http-chan))
        (->> (async/<! http-chan)
             (async/>! ret-chan))))

    (async/go
      (let [[ret _] (async/alts! [ret-chan (async/timeout timeout-ms)])
            ret (if (nil? ret) ::timeout ret)]
        (deliver ret-promise ret)))

    ret-promise))

(defrecord DrandGroupClient
  [group-urls group-info timeout-seconds]

  impl/IDrand
  (info [_]
    (fastest-query group-urls timeout-seconds get-info))
  (getPublicRound [_ round]
    (fastest-query group-urls timeout-seconds (partial get-public-round round)))
  (roundAt [_ instant]
    (let [{:strs [genesis_time period]} group-info]
      (impl/round-at instant genesis_time period)))
  (entropyAt [this round]
    (-> (impl/getPublicRound this round)
        deref
        impl/find-randomness))
  (entropyWatch [this consume!]
    (let [{:strs [genesis_time period]} group-info
          callback (bound-fn [] ;; propagate bindings
                     (consume! (impl/entropyAt this nil)))
          dlay (impl/next-round-in genesis_time period)]
      (partial future-cancel
               (-> (Executors/newSingleThreadScheduledExecutor)
                   (.scheduleAtFixedRate callback dlay period TimeUnit/SECONDS)))))
  )

(defn wrap
  "Returns a new drand client given the provided
   group <urls> and <timeout-seconds>."
  [urls timeout-seconds]
  (let [group-info (map #(deref (get-info :url % :timeout-seconds timeout-seconds)) urls)]
    (if (apply not= group-info)
      (throw
        (IllegalStateException. "Invalid group-info detected!"))
      (DrandGroupClient. urls (first group-info) timeout-seconds))))
