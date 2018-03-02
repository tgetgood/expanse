(ns chase.core
  "Simple animation compositing example"
  ;; TODO: animations.
  (:require [ubik.core :as l]
            [elections-demo.core :refer [annular-wedge]]
            [pixel.core :refer [blinky]]
            [ubik.system :as system]))

#?(:cljs (enable-console-print!))

(defn pacman [angle]
  [(assoc annular-wedge
          :inner-radius 0
          :outer-radius 10
          :from angle
          :to (- angle)
          :style {:stroke :black
                  :fill :yellow})
   (assoc l/circle
          :centre [1.5 6]
          :radius 1
          :style {:fill :black})])

(def pacman-open
  (map pacman (range 0.5 0.6 0.05)))

(def chomp
  (with-meta
    (apply concat
           (repeat (concat pacman-open (reverse pacman-open))))
    {:animation true :framerate 60}))

(def base-blinky
  (-> blinky
      (l/scale 1.2)
      (l/translate [40 7])))

(def base-pacman
  (-> (pacman 0.3)
      (l/translate [15 15])))

(def base
  (-> [base-pacman base-blinky]
      (l/scale 2)
      (l/translate [0 0])))

(defn composite
  "Returns the composite of the given animations streams. Later streams occlude
  earlier in each frame."
  [& animations]
  ;; TODO: What if we try to mix animations with different framerates? Well
  ;; that's is easy, but do it.
  (with-meta (partition (count animations) (apply interleave animations))
    {:animation true :framerate 60}))

(defn ^:export init []
  (system/initialise!
   {:size   :fullscreen
    :render (-> (first chomp)
                (l/scale 20)
                (l/translate [300 300]))}))

(defn on-reload []
  (on-reload))
