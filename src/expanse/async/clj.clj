(ns expanse.async.clj)

(defmacro async
  [& body]
  `(let [out# (clojure.core.async/chan)]
     (clojure.core.async/go
       (let [res# (do ~@body)]
         (clojure.core.async/>! out# res#)))
     out#))

(defmacro await [ch]
  `(clojure.core.async/<! ch))
