(ns expanse.async
  (:require #?(:clj [expanse.async.clj :as impl]
               :cljs [expanse.async.cljs :as impl :include-macros true])))

(defmacro async [body]
  `(impl/async ~@body))

(defmacro await [thing]
  `(impl/await ~thing))
