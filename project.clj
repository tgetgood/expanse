(defproject expanse "0.1.0-SNAPSHOT"
  :description "Package browser for Lemonade ecosystem."
  :url "https://lemonade.macroexpanse.com"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :min-lein-version "2.7.1"

  :dependencies [[org.clojure/clojure "1.9.0-RC2"]
                 [org.clojure/clojurescript "1.9.946"]]

  :plugins [[lein-figwheel "0.5.14"]
            [lein-cljsbuild "1.1.7" :exclusions [[org.clojure/clojure]]]]

  :source-paths ["src"]

  :cljsbuild
  {:builds
   [{:id           "dev"
     :source-paths ["src"]

     :figwheel     {:on-jsload "expanse.core/on-js-reload"}

     :compiler     {:main                 expanse.core
                    :asset-path           "js/compiled/out"
                    :output-to            "resources/public/js/compiled/expanse.js"
                    :output-dir           "resources/public/js/compiled/out"
                    :parallel-build       true
                    :checked-arrays       :warn
                    :source-map-timestamp true
                    :preloads             [devtools.preload]}}
    {:id           "min"
     :source-paths ["src"]
     :compiler     {:output-to      "resources/public/js/compiled/expanse.js"
                    :main           expanse.core
                    :parallel-build true
                    :checked-arrays :warn
                    :optimizations  :advanced
                    :pretty-print   false}}]}

  :profiles
  {:prod {:dependencies [macroexpanse/lemonade "0.1.0"]}
   :dev  {:dependencies  [[binaryage/devtools "0.9.4"]
                          [org.clojure/spec.alpha "0.1.134"]
                          [org.clojure/tools.namespace "0.2.11"]
                          [org.clojure/core.async "0.3.465"]
                          [figwheel-sidecar "0.5.14"
                           :exclusions [org.clojure/core.async]]
                          [com.cemerick/piggieback "0.2.2"]
                          [org.clojure/test.check "0.9.0"]]
          ;; Convenience for developing two pacakages in tandem
          :source-paths  ["src" "dev" "../lemonade/src"]
          :repl-options  {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
          ;; need to add the compliled assets to the :clean-targets
          :clean-targets ^{:protect false} ["resources/public/js/compiled"
                                            :target-path]}})