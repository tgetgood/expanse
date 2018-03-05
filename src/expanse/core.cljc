(ns expanse.core
  #?(:cljs (:require-macros [ubik.interactive.core :as spray]))
  (:require [clojure.string :as string]
            [expanse.fetch :as fetch]
            [ubik.core :as l]
            [ubik.geometry :as geo]
            [ubik.math :as math]
            [ubik.interactive.core :as spray]
            [ubik.interactive.system :as system]
            [ubik.transformation :as tx]
            [ubik.hosts :as hosts]))

#?(:cljs (enable-console-print!))

(defonce app-state (atom {:examples [] ::scroll 500}))

(defn frames [n dim]
  (map (fn [i]
         [(* dim (mod i n)) (* -1 dim (quot i n))])
       (range)))

(def subimage-panels
  (spray/subscribed-shape [:ubik.core/window (fn [s] (map :render (:examples s)))]
    (fn [window examples]
      (let [{:keys [width]} window
            frame-width 500
            cut 1300
            n (max 1 (quot width frame-width))
            dim (min width frame-width)
            offsets (frames n dim)
            sf (/ dim width)]
        (->
         (map-indexed
          (fn [i [offset render]]
            (-> [(-> l/frame
                     (assoc :width cut :height cut
                            :base-shape render)
                     (l/scale (/ width cut)))
                 (-> l/rectangle
                     (assoc :width width :height width)
                     (l/tag ::example-pane i))]
                (l/scale sf)
                (l/translate offset)))
          (partition 2 (interleave offsets examples)))
         (l/translate [(/ (- width (* n dim)) 2) 0]))))))

(defn scrolled [img]
  (println "img")
  (spray/subscribed-shape [::scroll]
    (fn [scroll]
      (println scroll)
      (l/translate img [0 scroll]))))

(declare system)

(def code-background
  (assoc l/rectangle :style {:fill "#E1E1E1"
                             :stroke "rgba(0,0,0,0)"}))

(defn format-code
  [code]
  (string/split-lines code))

(defn set-code [code h]
  (let [lines       (take (quot h 16) (format-code code))
        line-height 16
        box-height  (* (inc (count lines)) line-height)
        num-width   (* 12 (inc (math/floor (math/log 10 (count lines)))))]
    [(l/scale code-background [(+ num-width 600 5) box-height])
     (assoc l/line :from [num-width 0] :to [num-width box-height])
     (l/with-style {:font "14px monospace"}
       (map-indexed (fn [i line]
                      (let [h (- box-height (* line-height (inc i)))]
                        [(assoc l/text :text (str (inc i)) :corner [5 h] )
                         (assoc l/text :text line :corner [(+ 5 num-width) h])]))
                    lines))]))

(defn sub-render [current]
  (spray/subscribed-shape [:ubik.core/window :examples ::code-hover]
    (fn [{:keys [height]} examples hover]
      (let [render (:render (nth examples current))
            w (l/translate render [630 0])
            c (or hover [])]
        [(with-meta w {:events {:key ::introspectable}})
         (l/with-style {:fill :blue :opacity 0.3} c)
         (set-code (tx/friendlify-code c) height)]))))

(def embedding-window
  "Invisible pane used to control sub renders"
  (spray/subscribed-shape [:ubik.core/window]
    (fn [{:keys [width height]}]
      (-> l/rectangle
          (assoc :width width
                 :height height
                 :style {:stroke :none
                         :fill :none
                         :opacity 0})
          (l/tag ::embedding-window)))))

(def handler
  (spray/subscribed-shape [:current]
    (fn [c]
      (if c
        [embedding-window
         (sub-render c)]
        (scrolled subimage-panels)))))

(defn nav!
  ([] (nav! nil))
  ([n]
   (swap! app-state assoc :current n)
   (system/initialise! system)
   nil))

(def event-map
  #:ubik.events
  {:left-mouse-down (fn [{:keys [location]} db]
                      {:swap! [assoc :current
                               (spray/lookup-tag ::example-pane location)]})

   :hover (fn [{:keys [location]} db]
            (let [shape (spray/find ::any location)]
              {:swap!
               [assoc ::code-hover
                (geo/retree (geo/effected-branches
                             location shape))]}))

   :wheel (fn [{:keys [location dy]} db]
            (let [scroll (+ (::scroll db) dy)]
              {:swap! [assoc ::scroll scroll]}))})

(def host #?(:cljs (hosts/html-canvas {})
             :clj (hosts/quil {})))

(def system
  {:app-db          app-state
   :render          handler
   :host            host
   :subscriptions   {}
   :event-handlers  event-map
   :effect-handlers {}})

(defn on-reload []
  (swap! app-state assoc :examples (fetch/demo-list))
  (system/initialise! system))

(defn ^:export init []
  (on-reload))

;;;;; Test code.

(defonce tester (atom nil))

(spray/defsubs ex

  {:current (do (println "yeaik") (:current (spray/sub :db)))
   :examples (:examples (spray/sub :db))
   :view (do (println "no cache")
             (nth (spray/sub :examples) (spray/sub :current)))
   :window ^:no-cache (do (println "win") (:window (spray/sub :db)))
   :window-height (:height (spray/sub :window))
   })
