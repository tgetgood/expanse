(ns expanse.core
  (:require [expanse.fetch :as fetch]
            [lemonade.core :as l]
            [lemonade.events.hlei :as hlei]
            [lemonade.hosts :as hosts]
            [lemonade.system :as system]
            [lemonade.window :as window]))

#?(:cljs (enable-console-print!))

(defonce app-state (atom {:examples [] ::scroll 500}))

(def host hosts/default-host)

(def scroll-handler
  #:lemonade.events
  {:scroll (fn [{:keys [dy]}]
             {:mutation [(fn [state]
                           (let [s (::scroll state)
                                 c (-> state :examples count)]
                             (assoc state ::scroll (max 550 (+ s dy)))))]})})

(defn scroll-wrap [render]
  (fn [state]
    (let [w (render state)]
      (with-meta
        (assoc
         (l/translate w [0 (::scroll state)])
         :lemonade.events/handlers scroll-handler)
        (meta w)))))

(def click-handler
  #:lemonade.events
  {:left-click
   (fn [{[x y] :location}]
     {:mutation
      [(fn [state]
         ;; REVIEW: This is duplicating the geometry calculation that laid out
         ;; the screen in the first place. This is basically a form of path
         ;; dependence but even worse.
         ;;
         ;; So we really do want to be able to attach behaviour to the vos at
         ;; vaious points in the tree. In this case we want to give each example
         ;; pane metadata (whatever it needs to set itself as main) and then
         ;; have some generic logic call the handler with the pane in which the
         ;; event takes place.
         ;;
         ;; So here's another thing the DOM does well enough that I need to
         ;; replicate it.
         (let [{:keys [height width]} (-> state :lemonade.core/window)
               num-ex (-> state :examples count)
               scroll (::scroll state)
               frame-width 500
               n (max 1 (quot width frame-width))
               dim (min width frame-width)
               ry (+ (- y) scroll frame-width)
               ox (/ (- width (* n dim)) 2)
               rx (- x ox)
               i (+ (quot rx dim) (* n (quot ry dim)))]
           (-> state
               (update :current (fn [c]
                                  (if c
                                    nil
                                    (when (and (< 0 ry)
                                               (< 0 rx (* n dim))
                                               (< -1 i num-ex))
                                      i))))
               (update :lemonade.core/window assoc :zoom 0 :offset [0 0]))))]})})

(defn click-wrap [render]
  (fn [state]
    (let [w (render state)]
      (with-meta
        (assoc (l/composite {} [w])
               :lemonade.events/handlers click-handler)
        (meta w)))))

(defn frames [n dim]
  (map (fn [i]
         [(* dim (mod i n)) (* -1 dim (quot i n))])
       (range)))

(defn panes [state]
  (let [{:keys [height width]} (:lemonade.core/window state)
        frame-width 500
        cut 1300
        n (max 1 (quot width frame-width))
        dim (min width frame-width)
        offsets (frames n dim)
        sf (/ dim width)]
    (->
     (map (fn [offset {:keys [render]}]
            (-> [(l/scale (assoc l/frame :width cut :height cut
                                 :contents (render state))
                          (/ width cut))
                 (assoc l/rectangle :width width :height width)]
                (l/scale sf)
                (l/translate offset)))
          offsets (:examples state))
     (l/translate [(/ (- width (* n dim)) 2) 0]))))

(declare system)

(defn handler [state]
  ;; HACK: Holy shit is this kludgy. Dynamically swap out the rendering system
  ;; from inside the handler callback?!?!? Seems to work for the time being...
  (if-let [c (:current state)]
    (let [{:keys [behaviour render]} (nth (:examples state) c)]
      (system/initialise!
       (assoc system
              :render    (with-meta
                           #(if (:current %)
                              (if (fn? render) (render %) render)
                              (do (system/initialise! system) []))
                           (meta render))
              :behaviour (comp behaviour click-wrap)))
      ;; Return empty shape.
      [])
    (panes state)))

(def system
  {:host      host
   :size      :fullscreen
   :app-db    app-state
   :render    handler
   :behaviour (comp hlei/wrap click-wrap scroll-wrap)})

(defn data-init! []
  (let [dl (fetch/demo-list)
        db-mods (->> dl (map :app-db) (remove nil?) (map deref) (apply merge))]
    (swap! app-state (fn [db] (merge db-mods db {:examples dl})))))

(defn on-reload []
  (system/initialise! system)
  (data-init!))

(defn ^:export init []
  (on-reload))
