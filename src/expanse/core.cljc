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

(defn panes [state]
  ;; IDEA: We want to register a handler on each pane; conceptually at least. In
  ;; the new format, we want to put a recognisable key on each pane, and attach
  ;; some ancilliary state. Then when something with that key registers a click,
  ;; we want to lookup the function tied to [key :click] and execute it on the
  ;; thing with key.
  ;;
  ;; This is half baked, but the best way to bake it is probably just to give it
  ;; a shot.
  (comment
    ;; This guy handles events just dandy. Gets the frame clicked in any case...
    (when (= (:type ev) ::left-mouse-up)
      (println (filter #(contains? % :expanse.core/events)
                          (map meta
                               (first (geo/effected-branches
                                       (:location ev)
                                       (state/world))))))))

  (let [{:keys [height width]} (:lemonade.core/window state)
        frame-width 500
        cut 1300
        n (max 1 (quot width frame-width))
        dim (min width frame-width)
        offsets (frames n dim)
        sf (/ dim width)]
    (->
     (map-indexed
      (fn [i [offset {:keys [render]}]]
        (-> (with-meta
              [(l/scale (assoc l/frame :width cut :height cut
                               :contents (render state))
                        (/ width cut))
               (assoc l/rectangle :width width :height width)]
              {:events {:key ::example-pane :index i}})
            (l/scale sf)
            (l/translate offset)))
      (partition 2 (interleave offsets (:examples state))))
     (l/translate [(/ (- width (* n dim)) 2) 0]))))

(declare system)

(def code-background
  (assoc l/rectangle :style {:fill "#E1E1E1"
                             :stroke "rgba(0,0,0,0)"}))

(def format-code
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

(defn sub-render [render]
  (fn [state]
    (if (:current state)
      (let [w (l/translate ((window/wrap-windowing render) state) [630 0])
            c (or (::code-hover state) [])]
        [(with-meta w {:events {:key ::introspectable}})
         (l/with-style {:fill :blue :opacity 0.3} c)
         (set-code (tx/friendlify-code c) (-> state :lemonade.core/window :height))])
      (do (system/initialise! system) []))))

(defn handler [state]
  ;; HACK: Holy shit is this kludgy. Dynamically swap out the rendering system
  ;; from inside the handler callback?!?!? Seems to work for the time being...
  (if-let [c (:current state)]
    (let [{:keys [render]} (nth (:examples state) c)
          {:keys [width height]} (:lemonade.core/window state)]
      (system/initialise!
       (assoc system :render (fn [state]
                               [(with-meta
                                  (assoc l/rectangle
                                         :width width
                                         :height height
                                         :style {:stroke :none
                                                 :fill :none
                                                 :opacity 0})
                                  {:events {:key ::embedding-window}})
                                ((sub-render render) state)])))
      ;; Return empty shape.
      [])
    (panes state)))

(defn nav!
  ([] (nav! nil))
  ([n]
   (swap! app-state assoc :current n)
   (system/initialise! system)
   nil))

(def event-map
  {::example-pane
   (merge hlei/handlers
          #:lemonade.events
          {:left-click (fn [_ _ shape]
                         {:mutation [assoc :current (-> shape
                                                        meta
                                                        :events
                                                        :index)]})})
   ::introspectable
   (merge hlei/handlers
          {:lemonade.events/hover (fn [{:keys [location]} state shape]
                                    {:mutation
                                     [assoc ::code-hover
                                      (geo/retree (geo/effected-branches
                                                   location shape))]})})

   ::window/window
   (merge hlei/handlers window/window-events)

   ::embedding-window
   (merge hlei/handlers
          {:lemonade.events/left-click (fn [_ _ _]
                                         {:mutation [dissoc :current]})})})


(def system
  {:host           host
   :size           :fullscreen
   :app-db         app-state
   :render         handler
   :event-handlers event-map})

(defn data-init! []
  (let [dl      (fetch/demo-list)
        db-mods (->> dl (map :app-db) (remove nil?) (map deref) (apply merge))]
    (swap! app-state (fn [db] (merge db-mods db {:examples dl})))))

(defn on-reload []
  (system/initialise! system)
  (data-init!))

(defn ^:export init []
  (on-reload))
