(ns drand-clj.impl
  (:require [drand-clj.urls :as urls]
            [clojure.data.json :as json])
  (:import (java.net.http HttpClient HttpRequest HttpResponse HttpResponse$BodyHandlers HttpTimeoutException)
           (java.net URI)
           (java.time Duration Instant)
           (java.util.function Consumer Function)))

(set! *warn-on-reflection* true)

(defprotocol IDrand
  (info [this])
  (getPublicRound [this round])
  (roundAt [this instant])
  (entropyAt [this round])
  (entropyWatch [this entropy-callback]))

(def ^:dynamic *http-client*
  ;; the default http-client
  (HttpClient/newHttpClient))

(defn request*
  (^HttpRequest [url]
   (request* url nil))
  (^HttpRequest [^String url timeout-seconds]
   (cond-> (HttpRequest/newBuilder)
           true (.uri (URI. url))
           timeout-seconds (.timeout (Duration/ofSeconds timeout-seconds))
           true (.header "Content-Type" "application/json")
           true .GET
           true .build)))

(defn info-request
  [api timeout]
  (request* (str api urls/info) timeout))

(defn public-latest-request
  [api timeout]
  (request* (str api urls/public-latest) timeout))

(defn send-request!
  ([request]
   (send-request! request nil))
  ([^HttpRequest r handle-response!]
   (let [^HttpClient client *http-client*]
     (if handle-response!
       (-> client
           (.sendAsync r (HttpResponse$BodyHandlers/ofString))
           (.thenApply  (reify Function
                          (apply [_ resp]
                            (let [^HttpResponse resp resp]
                              (if (= 200 (.statusCode resp))
                                (-> resp .body json/read-str)
                                ::fail)))))
           (.exceptionally (reify Function
                             (apply [_ resp]
                               (let [^Throwable resp resp]
                                 (if (instance? HttpTimeoutException resp)
                                   ::http-timeout
                                   resp)))))
           (.thenAccept (reify Consumer
                          (accept [_ body]
                            (handle-response! body))))
           .join)
       (.send client r (HttpResponse$BodyHandlers/ofString))))))

(defn- b16-bytes
  "Decodes the provided String <bs> from Base16 (i.e. hex).
   Returns byte-array."
  ^bytes [^String s]
  (let [bs (.toByteArray (BigInteger. s 16))
        bs-len (alength bs)
        proper-len (bit-shift-right (.length s) 1)]
    (cond
      (= bs-len proper-len)
      bs

      (> proper-len bs-len)
      (let [padding (- proper-len bs-len)
            ret (byte-array (+ padding bs-len)
                            (repeat padding (byte 0)))]
        (System/arraycopy bs 0 ret padding bs-len)
        ret)

      :else
      (byte-array (next bs)))))

(defn find-randomness [m]
  (when (map? m)
    (-> m
        (get "randomness")
        b16-bytes)))


(defn next-round-in
  ^long [genesis period]
  (let [epoch-seconds (.getEpochSecond (Instant/now))]
    (-> genesis
        (range (+ epoch-seconds period) period)
        last
        (- epoch-seconds))))

(defn round-at
  [^Instant i genesis period]
  (let [epoch-seconds (inc (.getEpochSecond i))]
    (count
      (range genesis epoch-seconds period))))

