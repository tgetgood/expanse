(ns expanse.core
  (:require [expanse.fetch :as fetch]
            [lemonade.core :as l]
            [lemonade.events.hlei :as hlei]
            [lemonade.hosts :as hosts]
            [lemonade.system :as system]
            [lemonade.window :as window]))

(defonce app-state (atom {:examples []}))

(def host
  hosts/default-host)

(defn pane [i {:keys [app-db handler]}]
  (-> (handler @app-db)
      (l/translate [(* i 400) 0])))

(defn handler [state]
  (map-indexed pane (:examples state)))

(def system
  {:host    host
   :app-db  app-state
   :handler (-> handler
                window/wrap-windowing
                hlei/wrap)})

(defn data-init! []
  (binding [lemonade.system/initialise! identity]
    (swap! app-state assoc :examples (mapv (fn [x] (x)) fetch/demo-list))))

(defn on-reload []
  (system/fullscreen host)
  (system/initialise! system)

  (data-init!))

(defn ^:export init []
  (on-reload))
