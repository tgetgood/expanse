(ns expanse.fetch
  (:require basic.core
            chase.core
            elections-demo.core
            infinite.core
            [ubik.system :as system]
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
   #_infinite.core/init])

(defn exec [x] (x))

(defn demo-list []
  (binding [ubik.system/initialise! identity]
    (->> demo-nses
         (mapv exec)
         (map system/with-defaults))))
