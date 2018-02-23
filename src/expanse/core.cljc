(ns expanse.core
  #?@(:clj
       [(:require
         [clojure.pprint :refer [pprint]]
         [clojure.string :as string]
         [expanse.fetch :as fetch]
         [lemonade.core :as l]
         [lemonade.events.hlei :as hlei]
         [lemonade.geometry :as geo]
         [lemonade.hosts :as hosts]
         [lemonade.math :as math]
         [lemonade.system :as system]
         [lemonade.transformation :as tx]
         [lemonade.window :as window])]
       :cljs
       [(:require
         [cljs.pprint :refer [pprint]]
         [clojure.string :as string]
         [expanse.fetch :as fetch]
         [lemonade.events.hlei :as hlei]
         [lemonade.core :as l]
         [lemonade.geometry :as geo]
         [lemonade.hosts :as hosts]
         [lemonade.math :as math]
         [lemonade.system :as system]
         [lemonade.transformation :as tx]
         [lemonade.window :as window])]))

#?(:cljs (enable-console-print!))

(defonce app-state (atom {:examples [] ::scroll 500}))

(def host hosts/default-host)

(defn frames [n dim]
  (map (fn [i]
         [(* dim (mod i n)) (* -1 dim (quot i n))])
       (range)))

;; REVIEW: I get the feeling that I should encourage anyone creating shapes to
;; use subscribed-shape as far down the tree as possible. The reason being that
;; if you make the subscription high up and pass bits down as parameters, then
;; we can't efficiently memoise the functions within your function, whereas
;; every call to subscribe can be trivially wrapped up in the signal graph and
;; be called only when absolutely necessary.
;;
;; The problem here is that this feels a lot like dependency injection or action
;; at a distance.
;;
;; On the one hand queries are made closest to the point of use and logic
;; doesn't need to be added to parcel up data and pass it down to children.
;;
;; On the other hand it means that for optimal efficiency you'll be encouraged
;; to possibly query the same thing over and over rather than passing it
;; down. This could encourage architectures where every single VO is a
;; subscription, most of which are redundant. This is analogous to my complaints
;; against Angular.
;;
;; However, you're not forced to use subscriptions. So when it makes sense to
;; pass down a value, then you should. After all, if a value is queried in a
;; parent, and then again in a child, then both need to rerender if the parent
;; does, so you may as well use a function. So now style guides are
;; important. That feels like a lack of design clarity.
;;
;; This isn't a new problem. Every React wrapper has it to some degree. Do I
;; query here, or query above and pass down? Maybe it's not unreasonable to
;; remain agnostic here and consider the choice design aesthetic.
(defn subscribed-shape
  {:style/indent [1 :form]}
  [subscriptions render-fn])

(def subimage-panels
  (subscribed-shape [:lemonade.core/window :examples]
    (fn [window examples]
      (let [{:keys [height width]} window
            frame-width 500
            cut 1300
            n (max 1 (quot width frame-width))
            dim (min width frame-width)
            offsets (frames n dim)
            sf (/ dim width)]
        (->
         (map-indexed
          (fn [i [offset sub-render]]
            (-> [(-> l/frame
                     (assoc :width cut :height cut
                            :base-shape sub-render)
                     (l/scale (/ width cut))
                     (l/tag ::example-pane i))
                 (assoc l/rectangle :width width :height width)]
                (l/scale sf)
                (l/translate offset)))
          (partition 2 (interleave offsets examples)))
         (l/translate [(/ (- width (* n dim)) 2) 0]))))))

(defn scrolled [img]
  (subscribed-shape [::scroll]
    (fn [scroll]
      (l/translate img scroll))))

(declare system)

(def code-background
  (assoc l/rectangle :style {:fill "#E1E1E1"
                             :stroke "rgba(0,0,0,0)"}))

(def format-code
  ;;FIXME: Faster, but massive memory strain.
  ;; I think we need a way to create signaly atoms on the fly. Reactions that
  ;; can be passed.
  ;; REVIEW: This is just one use case for a potentially horrifyingly ugly
  ;; kludge. Take your time. There's probably a simpler solution to this edge
  ;; case. Like an LRU or otherwise limited cache
  (memoize
   (fn [code]
     (string/split-lines (with-out-str (pprint code))))))

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
  (subscribed-shape [:lemonade.core/window :examples ::code-hover]
    (fn [{:keys [height]} examples hover]
      (let [render (get examples current [])
            w (l/translate (window/wrap-windowing render) [630 0])
            c (or hover [])]
        [(with-meta w {:events {:key ::introspectable}})
         (l/with-style {:fill :blue :opacity 0.3} c)
         (set-code (tx/friendlify-code c) height)]))))

(def embedding-window
  "Invisible pane used to control sub renders"
  (subscribed-shape [:lemonade.core/window]
    (fn [{:keys [width height]}]
      (-> l/rectangle
          (assoc :width width
                 :height height
                 :style {:stroke :none
                         :fill :none
                         :opacity 0})
          (l/tag ::embedding-window)))))

(def handler
  (subscribed-shape [:current]
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

(defn subscribe [a b])
(defn lookup-shapes [key])

(def panel-click-handler
  (subscribe [:lemonade.events/left-click]
             (fn [{:keys [location]}]
               (let [panes (lookup-shapes ::example-pane)
                     in (filter #(geo/contains? % location) panes)]
                 (when (seq panes)
                   (let [pane (first panes)
                         index (l/get-tag-data pane ::example-pane)]
                     {:mutation [assoc :current index]}))))))

(def event-map
  #:lemonade.events
  {:left-click (fn [_ _ shape]
                 {:mutation [assoc :current (-> shape
                                                meta
                                                :events
                                                :index)]})
   :lemonade.events/hover (fn [{:keys [location]} state shape]
                            {:mutation
                             [assoc ::code-hover
                              (geo/retree (geo/effected-branches
                                           location shape))]})

   ::ebedding-window
   (merge hlei/handlers
          {:lemonade.events/left-click (fn [_ _ _]
                                         {:mutation [dissoc :current]})})

   :basic.core/poly
   (merge hlei/handlers
          {:lemonade.events/mouse-move (fn [& args] (println args))})})

(def system
  {:host           host
   :size           :fullscreen
   :app-db         app-state
   :render         handler #_basic.core/ex
   :event-handlers event-map})

(defn on-reload []
  (swap! app-state assoc :examples (fetch/demo-list))
  (system/initialise! system))

(defn ^:export init []
  (on-reload))

(defonce tester (atom nil))
