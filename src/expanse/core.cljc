(ns expanse.core
  (:require [expanse.fetch :as fetch]
            [lemonade.core :as l]
            [lemonade.events.hlei :as hlei]
            [lemonade.hosts :as hosts]
            [lemonade.system :as system]
            [lemonade.window :as window]))

#?(:cljs (enable-console-print!))

(defonce app-state (atom {:examples []}))

(def ratio 1.9)

(def host
  hosts/default-host)

(defn panes [state]
  (let [{:keys [height width]} (:lemonade.core/window state)
        sf 0.75]
    (map-indexed
     (fn [i handler]
       (-> [(handler state)
            (assoc l/rectangle :height height :width width)]
           (l/scale sf)
           (l/translate [(* (- 1 sf) width)
                         (- (/ (* (- 1 sf) height) 2) (* i height sf))])))
     (:examples state))))

(defn handler [state]
  (if-let [c (:current @app-state)]
    (let [{:keys [handler app-db]} (nth (:examples @app-state) c)]
      (handler @app-db))
    (panes state)))

(def system
  {:host    host
   :app-db  app-state
   :handler (-> handler
                window/wrap-windowing
                hlei/wrap)})

(defn data-init! []
  (binding [lemonade.system/initialise! identity]
    (let [sub-images (mapv (fn [x] (x)) fetch/demo-list)
          db (apply merge (map deref (map :app-db sub-images)))
          handlers (map :handler sub-images)]
      (swap! app-state (fn [old] (merge db old {:examples handlers}))))))

(defn on-reload []
  (system/fullscreen host)
  (system/initialise! system)
  (data-init!))

(defn ^:export init []
  (on-reload))
