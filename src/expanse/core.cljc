(ns expanse.core
  (:require [clojure.string :as string]
            [expanse.fetch :as fetch]
            [lemonade.core :as l]
            [lemonade.events.hlei :as hlei]
            [lemonade.geometry :as geo]
            [lemonade.math :as math]
            [lemonade.spray :as can]
            [lemonade.system :as system]
            [lemonade.transformation :as tx]
            [lemonade.window :as window]))

#?(:cljs (enable-console-print!))

(defonce app-state (atom {:examples [] ::scroll 500}))

(defn frames [n dim]
  (map (fn [i]
         [(* dim (mod i n)) (* -1 dim (quot i n))])
       (range)))

(def subimage-panels
  (can/subscribed-shape [:lemonade.core/window (fn [s] (map :render (:examples s)))]
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
                     (l/scale (/ width cut))
                     (l/tag ::example-pane i))
                 (assoc l/rectangle :width width :height width)]
                (l/scale sf)
                (l/translate offset)))
          (partition 2 (interleave offsets examples)))
         (l/translate [(/ (- width (* n dim)) 2) 0]))))))

(defn scrolled [img]
  (can/subscribed-shape [::scroll]
    (fn [scroll]
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
  (can/subscribed-shape [:lemonade.core/window :examples ::code-hover]
    (fn [{:keys [height]} examples hover]
      (let [render (get examples current [])
            w (l/translate (window/wrap-windowing render) [630 0])
            c (or hover [])]
        [(with-meta w {:events {:key ::introspectable}})
         (l/with-style {:fill :blue :opacity 0.3} c)
         (set-code (tx/friendlify-code c) height)]))))

(def embedding-window
  "Invisible pane used to control sub renders"
  (can/subscribed-shape [:lemonade.core/window]
    (fn [{:keys [width height]}]
      (-> l/rectangle
          (assoc :width width
                 :height height
                 :style {:stroke :none
                         :fill :none
                         :opacity 0})
          (l/tag ::embedding-window)))))

(def handler
  (can/subscribed-shape [:current]
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

(defn lookup-shapes [key])

(def panel-click-handler
  (can/subscribed-shape [:lemonade.events/left-click]
    (fn [{:keys [location]}]
      (let [panes (lookup-shapes ::example-pane)
            in (filter #(geo/contains? % location) panes)]
        (when (seq panes)
          (let [pane (first panes)
                index (l/get-tag-data pane ::example-pane)]
            {:mutation [assoc :current index]}))))))

(def event-map
  #:lemonade.events
  {:left-mouse-down (fn [e db]
                      (let [shape (spray/find ::example-pane location)]
                        {:swap! [assoc :current (-> shape
                                                    meta
                                                    :events
                                                    :index)]}))
   :lemonade.events/hover (fn [{:keys [location]} db]
                            (let [shape (spray/find ::any location)]
                              {:swap!
                               [assoc ::code-hover
                                (geo/retree (geo/effected-branches
                                             location shape))]}))})

(def system
  {:size           :fullscreen
   :app-db         app-state
   :render         handler #_basic.core/ex
   :event-handlers event-map})

(defn on-reload []
  (swap! app-state assoc :examples (fetch/demo-list))
  (system/initialise! system))

(defn ^:export init []
  (on-reload))

(defonce tester (atom nil))
