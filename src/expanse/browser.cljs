(ns expanse.browser
  (:require [goog.object :as obj]
            [lemonade.hosts :as hosts]))

(defn canvas-elem []
  (js/document.getElementById "canvas"))

(defn canvas-container []
  (js/document.getElementById "canvas-container"))

(defn canvas-container-dimensions []
  (let [cc (canvas-container)]
    [(obj/get cc "clientWidth") (obj/get cc "clientHeight")]))

(defn set-canvas-size! [canvas [width height]]
  (obj/set canvas "width" width)
  (obj/set canvas "height" height))

(defn fullscreen! []
  (let [[w h :as dim] (canvas-container-dimensions)]
    (set-canvas-size! (canvas-elem) dim)))

(defn host []
  (hosts/html-canvas (canvas-elem)))
