(defproject drand-clj "0.2.0"
  :description "Clojure http-client to the `drand` randomness beacon network."
  :url "https://github.com/jimpil/drand-clj"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure      "1.10.1" :scope "provided"]
                 [org.clojure/data.json    "1.0.0"]
                 [org.clojure/core.async   "1.3.610"]
                 [org.clojure/core.memoize "1.0.236"]]
  :repl-options {:init-ns drand-clj.core}

  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag" "--no-sign"]
                  ["deploy"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ;["vcs" "push"]
                  ]
  :deploy-repositories [["releases" :clojars]] ;; lein release :patch
  :signing {:gpg-key "jimpil1985@gmail.com"})
