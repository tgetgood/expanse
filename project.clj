(defproject expanse "0.1.0"
  :description "Demo browser for Ubik ecosystem."
  :url "https://github.com/tgetgood/expanse"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :min-lein-version "2.7.1"

  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.9.946"]]

  :plugins [[lein-figwheel "0.5.14"]
            [lein-cljsbuild "1.1.7" :exclusions [org.clojure/clojure]]]

  :source-paths ["src"
                 "examples/chase/src"
                 "examples/pixel/src"
                 "examples/elections-demo/src"
                 "examples/basic/src"
                 "examples/infinite/src"]

  :cljsbuild
  {:builds
   [{:id           "dev"
     :source-paths ["src"
                    "../ubik/src"
                    "examples/elections-demo/src"
                    "examples/basic/src"
                    "examples/chase/src"
                    "examples/pixel/src"
                    "examples/infinite/src"]

     :figwheel     {:on-jsload "expanse.core/on-reload"}

     :compiler     {:main                 expanse.core
                    :asset-path           "js/compiled/out"
                    :output-to            "resources/public/js/compiled/expanse.js"
                    :output-dir           "resources/public/js/compiled/out"
                    :parallel-build       true
                    :checked-arrays       :warn
                    :source-map-timestamp true
                    :preloads             [devtools.preload]}}

    {:id           "min"
     :source-paths ["src"
                    "examples/chase/src"
                    "examples/pixel/src"
                    "examples/elections-demo/src"
                    "examples/basic/src"
                    "examples/infinite/src"]
     :compiler     {:output-to      "resources/public/js/compiled/expanse.js"
                    :asset-path     "js/compiled/out"
                    :main           expanse.core
                    :parallel-build true
                    :checked-arrays :warn
                    :optimizations  :advanced
                    :pretty-print   false}}]}

  :profiles
  {:prod {:dependencies [[macroexpanse/ubik "0.3.0"]]}
   :dev  {:dependencies  [[binaryage/devtools "0.9.9"]
                          [quil "2.6.0" :exclusions [[org.clojure/clojure]]]
                          [org.clojure/spec.alpha "0.1.134"]
                          [org.clojure/tools.namespace "0.2.11"]
                          [figwheel-sidecar "0.5.14"
                           :exclusions [org.clojure/core.async]]
                          [org.clojure/core.async "0.3.465"]
                          [com.cemerick/piggieback "0.2.2"]
                          [org.clojure/test.check "0.9.0"]]
          ;; need to add dev source path here to get user.clj loaded
          :source-paths  ["src"
                          "dev"
                          "../ubik/src"
                          "examples/chase/src"
                          "examples/pixel/src"
                          "examples/basic/src"
                          "examples/infinite/src"
                          "examples/elections-demo/src"]

          :repl-options  {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
          ;; need to add the compliled assets to the :clean-targets
          :clean-targets ^{:protect false} ["resources/public/js/compiled"
                                            :target-path]}})
