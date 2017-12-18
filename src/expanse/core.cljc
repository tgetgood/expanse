(ns expanse.core
  #?(:clj (:refer-clojure :exclude [await]))
  (:require [expanse.eval :as eval]
            [expanse.fetch :as fetch]
            [expanse.async :refer [async await]]
            [lemonade.core :as l]
            [lemonade.hosts :as hosts]
            [lemonade.system :as system]))

;; define your app data so that it doesn't get over-written on reload


(defonce app-state (atom {:text "Hello world!"}))

(defn init-sub [ns vars]
  (swap! app-state assoc :sub
         (binding [lemonade.system/initialise! identity]
           ((symbol ns "init"))))
  )

(defn eval-sources [source]
  (async
   (let [code (first (await source))
         ns (eval/resolve-ns code)]
     (eval/eval-str code #(init-sub ns %)))))

(def host
  hosts/default-host)

(defn on-reload []
  (system/fullscreen host)

  (system/initialise!
   {:host    host
    :app-db  app-state
    :handler (fn [_] (assoc l/circle :centre [200 100] :radius 300))}

   (init-sub "election-demos.core" (eval-sources (fetch/demos)))))

(defn ^:export init []
  (on-reload))
