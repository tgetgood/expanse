(ns pixel.core
  "Create pixel sprites and then manipulate them as if they were vector images."
  (:require [lemonade.core :as l]
            [lemonade.system :as system]))

#?(:cljs (enable-console-print!))

(l/deftemplate pixel
  {:colour :black
   :location [0 0]}
  (assoc l/rectangle
         :width 1
         :height 1
         :corner location
         :style {:stroke colour
                 :fill colour}))

(def grid
  (into #{}
        (mapcat (fn [x]
                  (map (fn [y]
                         [x y])
                    (range 14)))
                (range 14))))

(def eye
  [(map #(assoc pixel :colour :lightgrey :location %)
     [[1 3] [2 3]
      [0 2] [1 2] [2 2] [3 2]
      [2 1] [3 1]
      [2 0] [3 0]
      [1 -1] [2 -1]])
   (map (partial assoc pixel :colour :blue :location)
     [[0 0] [0 1] [1 0] [1 1]])])

(def masked
  [[1 0] [2 0] [3 0]
   [2 1]

   [6 0] [6 1]
   [7 0] [7 1]

   [10 0] [11 0] [12 0]
   [11 1]

   [0 8] [0 9] [0 10] [0 11] [0 12] [0 13]
   [1 11] [1 12] [1 13]
   [2 12] [2 13]
   [3 13]
   [4 13]

   [9 13] [10 13] [11 13] [12 13] [13 13]
   [11 12] [12 12] [13 12]
   [12 11] [13 11]
   [13 10]
   [13 9]
   [13 8]])

(def blinky
  [(map (partial assoc pixel :colour :red :location)
     (apply disj grid masked))
   (l/translate eye [1 7])
   (l/translate eye [7 7])])

(defn ^:export init []
  (system/initialise!
   ;; Make the sprite big enough to see without zooming in.
   {:render (l/scale blinky 40)}))

(defn on-reload []
  (on-reload))
