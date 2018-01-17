(ns basic.core
  (:require
   [lemonade.core :as l]
   [lemonade.hosts :as hosts]
   [lemonade.system :as system]
   [lemonade.window :as window]
   [lemonade.events.hlei :as hlei]))

#?(:cljs (enable-console-print!))

(def host hosts/default-host)

(def ex
  [(-> l/polyline
       (assoc :points [[0 0] [100 100] [300 100] [100 300] [0 0]]
              :style {:stroke :cyan
                      :fill   :purple})
       (l/scale 3)
       (l/rotate 20)
       (l/translate [300 40]))
   (assoc l/line :from [800 100] :to [900 100])
   (l/with-style {:fill :pink}
     (-> l/annulus
         (assoc :outer-radius 300
                :inner-radius 200
                :style {:fill   :red
                        :stroke :blue})
         (l/translate [500 500])))

   (l/scale l/circle [4000 500])])

(defn on-reload []
  ;; REVIEW: Using a draw loop to draw a static image. Wasteful (a little) &
  ;; confusing that you have to wrap the static image in an fn. What to do with
  ;; static images? Stateless animations?
  (system/initialise!
   {:host      host
    :size      :fullscreen
    :handler   (constantly ex)
    :behaviour (comp hlei/wrap window/wrap-windowing)}))

(defn ^:export init []
  (on-reload))
