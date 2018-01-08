(ns expanse.fetch
  (:require elections-demo.core))

(def demo-list
  ;; HACK:
  "hard coded list of demo init fns"
  [elections-demo.core/init-histogram elections-demo.core/init-circles])
