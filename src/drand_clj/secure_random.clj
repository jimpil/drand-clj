(ns drand-clj.secure-random
  (:require [drand-clj.client :as client])
  (:import (java.security SecureRandom)))

(defn strong-random
  "Returns the strongest possible instance of `SecureRandom`
   with 32 bytes of added <entropy> (supplementing its seed).
   The no-arg arity uses the default (Cloudflare) `drand` beacon."
  (^SecureRandom []
   (strong-random (client/entropy)))
  (^SecureRandom [^bytes entropy]
   (let [srand (SecureRandom/getInstanceStrong)]
     (.nextInt srand) ;; force self-seeding
     (doto srand
       (.setSeed entropy)))))
