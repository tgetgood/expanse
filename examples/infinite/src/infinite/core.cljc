(ns infinite.core
  (:require
   [lemonade.core :as l]
   [lemonade.hosts :as hosts]
   [lemonade.math :as math]
   [lemonade.system :as system]
   [lemonade.window :as window]
   [lemonade.events.hlei :as hlei]))

#?(:cljs (enable-console-print!))

(def factor (/ 36 64))

(def r 7)

(defn nth-ring [n]
  (-> l/annulus
      (assoc :style {:fill :rebeccapurple
                     :stroke :none}
             :inner-radius 6
             :outer-radius 8)
      (l/scale (math/exp factor (- n)))))

(defn contains-origin? [x y w h]
  (and (<= 0 x w) (<= 0 y h)))

(defn abs-dist [f x y w h]
  (math/sqrt (+ (math/exp (f (math/abs x)
                                     (math/abs (- w x))) 2)
                    (math/exp (f (math/abs y)
                                     (math/abs (- h y))) 2))))

(defn logit [n z]
  (math/floor (math/log (/ 1 factor) (/ n (* z r)))))

(defn rings [{:keys [zoom offset width height]}]
  (let [z (window/normalise-zoom zoom)
        [x y] offset]
    (let [M (abs-dist max x y width height)
          N (+ 2 (logit M z))
          m 1
          n (logit m z)]
      (map nth-ring (range n N)))))


(defn example [state]
  (if-let [window (:lemonade.core/window state)]
    (rings window)
    []))

(defn ^:export init []
  (system/initialise!
   {:host      hosts/default-host
    :size      :fullscreen
    :handler   example
    :behaviour (comp hlei/wrap window/wrap-windowing)}))

(defn on-reload []
  (init))
