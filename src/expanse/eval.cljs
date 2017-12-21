(ns expanse.eval
  (:require [cljs.reader :refer [read-string]]
            [clojure.string :as string]
            [eval-soup.core :as eval-soup]))

(defn resolve-ns [s]
  (let [ns-form (read-string s)]
    (if (= (first ns-form) 'ns)
      (second (read-string s))
      'expanse.eval.evaluator)))

(defn resolve-ns-keywords [s]
  (let [the-ns (resolve-ns s)]
    (string/replace s #"::" (str ":" the-ns "/"))))

(defn read-all-forms [s]
  (let [s' (resolve-ns-keywords s)]
    (read-string (str "[" s' "]"))))

(def eval-ns
  "Dummy ns in which to eval things. Not sure what the point of this is..."
  (do (create-ns 'expanse.eval.evaluation)
      (atom 'expanse.eval.evaluation)))

(defn- prepend-path [opts]
  (update opts :path #(str "/js/compiled/out/" %)))

(defn load-fn [opts cb]
  (if (re-matches #"^goog/.*" (:path opts))
    (eval-soup/custom-load! (-> opts
                                (update :path eval-soup/fix-goog-path)
                                prepend-path)
                            [".js"]
                            cb)
    (eval-soup/custom-load! (prepend-path opts)
                            (if (:macros opts)
                              [".cljc" ".clj"]
                              [".js" ".cljc" ".cljs"])
                            cb)))

(defn eval [ns forms cb]
  (eval-soup/eval-forms
   (map eval-soup/wrap-macroexpand forms)
   cb
   eval-soup/state
   ns
   load-fn))

(set! cljs.js/*eval-fn* cljs.js/js-eval)
(defonce state eval-soup/state)

(defn code->results
  ([code cb]
   (code->results code cb {}))
  ([code cb {:keys [custom-load current-ns]
              :or {custom-load load-fn
                   current-ns (atom (resolve-ns code))}}]
   (let [forms (read-all-forms code)
         eval-cb (fn [results]
                   (cb results))
         read-cb (fn [results]
                   (eval-soup/eval-forms
                    (eval-soup/add-timeouts-if-necessary forms results)
                     eval-cb
                     state
                     current-ns
                     custom-load))
         init-cb (fn [results]
                   (eval-soup/eval-forms
                    forms
                    read-cb
                    state
                    current-ns
                    custom-load))]
     (eval-soup/eval-forms
      ['(ns cljs.user)
       '(def ps-last-time (atom 0))
       '(defn ps-reset-timeout! []
          (reset! ps-last-time (.getTime (js/Date.))))
       '(defn ps-check-for-timeout! []
          (when (> (- (.getTime (js/Date.)) @ps-last-time) 5000)
            (throw (js/Error. "Execution timed out."))))
       '(set! *print-err-fn* (fn [_]))
       (list 'ns @current-ns)]
      init-cb
      state
      current-ns
      custom-load))))

(defn eval-str
  "Evaluates the string content of a clojure file. Returns a vector of evaluated
  forms with the var init last in the list. This is a kludge to load the file."
  [s cb]
  (code->results s cb))
