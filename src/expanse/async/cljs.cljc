(ns expanse.async.cljs)

(defmacro async
  [& body]
  `(let [out# (cljs.core.async/chan)]
     (cljs.core.async/go
       (let [res# (do ~@body)]
         (cljs.core.async/>! out# res#)))
     out#))

(defmacro await [ch]
  `(cljs.core.async/<! ch))
