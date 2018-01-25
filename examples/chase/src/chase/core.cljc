(ns chase.core
  (:require [lemonade.core :as l]
            [lemonade.system :as system]))

#?(:cljs (enable-console-print!))

(defn image-fn [_]
  )

(defn ^:export init []
  (system/initialise!
   {:size   :fullscreen
    :render []}))

(defn on-reload []
  (on-reload))
