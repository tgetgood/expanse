(ns basic.core
  (:require [ubik.core :as l]
            [ubik.system :as system]))

#?(:cljs (enable-console-print!))

(def ex
  [(-> l/polyline
       (assoc :points [[0 0] [100 100] [300 100] [100 300] [0 0]]
              :style {:stroke :cyan
                      :fill   :purple})
       (l/tag ::poly)
       (l/scale 3)
       (l/rotate 20)
       (l/translate [300 40])
       (l/tag ::translate))
   (assoc l/line :from [800 100] :to [900 100])
   (assoc l/arc :centre [0 0] :radius 200 :style {:stroke :red} :from 0 :to 1)
   (l/with-style {:fill :pink}
     (-> l/annulus
         (assoc :outer-radius 300
                :inner-radius 200
                :style {:fill   :red
                        :stroke :blue})
         (l/translate [500 500])))

   (l/scale l/circle [4000 500])])

(defn ^:export init []
  (system/initialise!
   {:size   :fullscreen
    :render ex}))

(defn on-reload []
  (init))
