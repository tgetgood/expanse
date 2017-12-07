(ns expanse.macros
  (:require [clojure.pprint :refer [pprint pp]])
  (:refer-clojure :exclude [await]))

(defmacro async
  [& body]
  `(let [out# (cljs.core.async/chan)]
     (cljs.core.async/go
       (let [res# (do ~@body)]
         (cljs.core.async/>! out# res#)))
     out#))

(defmacro await
  ""
  {:style/indent 1}
  [bindings & body]
  `(let [~@(mapcat (fn [[bind# val#]]
                     (list bind# (list 'cljs.core.async/<! val#)))
                   (partition 2 bindings))]
     ~@body))
