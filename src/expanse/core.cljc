(ns expanse.core
  (:require [expanse.fetch :as fetch]
            [lemonade.core :as l]
            [lemonade.hosts :as hosts]
            [lemonade.system :as system]))

;; define your app data so that it doesn't get over-written on reload


(defonce app-state (atom {:text "Hello world!"}))

(fetch/demos #(swap! app-state assoc :raw %))

(def host
  hosts/default-host)

(defn on-reload []
  (system/fullscreen host)

  (system/initialise!
   {:host    host
    :app-db  app-state
    :handler (fn [_] (assoc l/circle :centre [200 100] :radius 300))}))

(defn ^:export init []
  (on-reload))
