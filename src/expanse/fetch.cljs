(ns expanse.fetch
  (:require [cljs.pprint :refer [pprint]]
            [cljs.core.async :as async
             :refer [<! >! put! chan] :refer-macros [go]]
            [goog.object :as obj])
  (:require-macros [expanse.macros :refer [async await await-all]])
  (:import [goog.net XhrIo]))

(def demo-list
  ;; HACK:
  "hard coded list of demos."
  ["index.html" "packages/elections-demo.html" "packages/infinite.html"])

(defn fetch [url]
  (fn [cb]
    (.send XhrIo url (fn [e]
                       (-> e
                           .-target
                           .getResponseText
                           cb)))))

(defn cb->chan [f]
  (let [out (chan)]
    (f #(put! out %))
    out))

(defn demos [cb]
  (let [chans  (mapv #(cb->chan (fetch %)) demo-list)
        middle (chan)
        out    (async/into [] middle)]
    (go
      (loop [[ch & more] chans]
        (when ch
          (>! middle (<! ch))
          (recur more)))
      (async/close! middle)
      (cb (<! out)))))
