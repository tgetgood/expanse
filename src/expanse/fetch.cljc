(ns expanse.fetch
  (:require elections-demo.core
            [basic.core :as basic]
            [infinite.core :as infinite]))

(def demo-list
  ;; HACK:
  "hard coded list of demo init fns"
  [elections-demo.core/init-histogram elections-demo.core/init-circles
   basic/init
   infinite/init])
