(ns expanse.core
  #?@(:clj
       [(:require
         [lemonade.core :as l]
         [lemonade.events :as events]
         [lemonade.system :as system])]
       :cljs
       [(:require
         [expanse.browser :as browser]
         [lemonade.core :as l]
         [lemonade.events :as events]
         [lemonade.system :as system])]))

;; define your app data so that it doesn't get over-written on reload

(defonce app-state (atom {:text "Hello world!"}))

(def host
  #?(:cljs (browser/host)
     :clj {}))

(def set-fullscreen!
  #?(:cljs browser/fullscreen!
     :clj (constantly nil)))

(defn on-reload []
  (set-fullscreen!)

  (system/initialise!
   {:host    host
    :app-db  app-state
    :handler (fn [_] (assoc l/circle :centre [0 0] :radius 300))}))

(defn ^:export init []
  (on-reload))
