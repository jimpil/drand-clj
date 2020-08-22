(ns drand-clj.client-test
  (:require [clojure.test :refer :all]
            [drand-clj.core :as drand]))


(deftest client-tests

  (let [client (drand/client-for)]

    (testing "beacon info"
      (is (= (:group-info client)
             @(drand/get-info client))))

    (testing "beacon public latest randomness"
      (is (= 4 (count @(drand/get-public client)))))

    (testing "beacon public randomness - round 51"
      (is (= @(drand/get-public client 51)
             {"round" 51,
              "randomness" "ee65a48edd4a0859a715f8050d10c707c44a3b73ae97817215339784a30e49fb",
              "signature" "837807c54b56b71777d5a671de7a4dfb5b79523cb3ad16cbaa4bdcbf3f706c6637d85a84d1e2c51f7e26526a2fb4b9f913ef8d2ccf21bf6f16e50ca0e7ca2e75b33ad9a6ebc9b7219a5dae651067bcb0754efb69c3bb98922d3af751a3bf5805",
              "previous_signature" "8f0b35b4072d550abff3924b9a5dbc0cd3f7810b08dd417494bb7a955385991e7091b91762681aa8ba0901715d35278812eb4e915bb3d4071aef9ea3630e5638bb59aa276bc6b82e713c6791c7edb833db40633e02a88c26282f7101e655dd19"}
             )))
    )
  )
