(ns drand-clj.client-test
  (:require [clojure.test :refer :all]
            [drand-clj.client :as client]
            [drand-clj.urls :as urls]))

(def group-urls
  [urls/protocol-labs1
   urls/protocol-labs2
   urls/protocol-labs3
   urls/cloudflare])


(deftest url-tests
  (testing "beacon info"
    (doseq [api group-urls]
      (is (= urls/chain-hash (get (client/info api) "hash")))))

  (testing "beacon public"
    (doseq [api group-urls]
      (is (= 4 (count (client/get-public api))))))

  )
