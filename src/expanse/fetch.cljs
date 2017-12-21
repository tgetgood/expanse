(ns expanse.fetch
  (:require [cljs.core.async :as async :refer [<! >! chan put!] :refer-macros [go]]
            [expanse.eval :as eval])
  (:import goog.net.XhrIo))

(def demo-list
  ;; HACK:
  "hard coded list of demos."
  ["packages/elections-demo" #_"packages/infinite"])

(enable-console-print!)
(defn fetch [url]
  (fn [cb]
    (.send XhrIo url (fn [e]
                       (if (-> e
                               .-target
                               .getStatus
                               (= 200))
                         (-> e
                             .-target
                             .getResponseText
                             cb)
                         (cb nil))))))

(defn cb->chan [f]
  (let [out (chan)]
    (f (fn [v] (if (nil? v)
                 (async/close! out)
                 (put! out v))))
    out))


(defn grab-sources [path]
  (-> path
      (str "/core.cljc")
      fetch
      cb->chan))

(defn demos []
  (let [chans  (mapv grab-sources demo-list)
        middle (chan)
        out    (async/into [] middle)]
    (go
      (loop [[ch & more] chans]
        (when ch
          (let [v (<! ch)]
            (when-not (nil? v)
              (>! middle v)))
          (recur more)))
      (async/close! middle))
    out))


(defn source [cb]
  (go
    (let [code (first (<! (demos)))
          ns (eval/resolve-ns code)]
      (eval/eval-str code #(cb ns %)))))
