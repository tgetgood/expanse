(ns expanse.fetch
  (:require basic.core
            chase.core
            elections-demo.core
            infinite.core
            [lemonade.events.hlei :as hlei]
            [lemonade.window :as window]
            pixel.core))

(def demo-nses
  ;; HACK:
  "Hard coded list of demos for now. These are the nses that would be pointed to
  by :main in a Leiningen project.clj file. Instead of searching for -main, we
  search for init, but that might change."
  [elections-demo.core/init
   elections-demo.core/init-circles
   pixel.core/init
   chase.core/init
   basic.core/init
   infinite.core/init])

(def default-behaviour (comp hlei/wrap window/wrap-windowing))

(defn exec [x] (x))

(defn demo-list []
  (binding [lemonade.system/initialise! identity]
    (->> demo-nses
         (mapv exec)
         (map #(update % :behaviour (fnil identity default-behaviour))))))
