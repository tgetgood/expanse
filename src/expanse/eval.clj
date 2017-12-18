(ns expanse.eval
  (:refer-clojure :exclude [eval]))

(defn resolve-ns [s]
  (second (read-string s)))

(defn eval [forms cb]
  (cb (mapv clojure.core/eval forms)))

(defn eval-str [s cb]
  ;; FIXME: Need to read all forms.
  (cb (eval (read-string s))))
