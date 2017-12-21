(ns expanse.core
  (:require [clojure.pprint :refer [pprint]]
            [expanse.fetch :as fetch]
            [lemonade.core :as l]
            [lemonade.draw :as draw]
            [lemonade.hosts :as hosts]
            [lemonade.system :as system]))

(defonce app-state (atom {:text "Hello world!"}))

(defn tester-drawer [ns {:keys [handler app-db]}]
  (system/stop!)
  (draw/draw! (handler @app-db)))

(defn init-sub [ns vars]
  (pprint vars)
  (binding [lemonade.system/initialise! (partial tester-drawer ns)]
    #_((last vars))))

(def host
  hosts/default-host)

(defn on-reload []
  (system/fullscreen host)

  (system/initialise!
   {:host    host
    :app-db  app-state
    :handler (fn [_] (assoc l/circle :centre [200 100] :radius 300))})

  (fetch/source init-sub))

(defn ^:export init []
  (on-reload))
