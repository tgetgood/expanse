(ns expanse.eval
  (:require [cljs.reader :refer [read-string]]
            [clojure.string :as string]
            [eval-soup.core :as eval-soup]))

(defn resolve-ns [s]
  (let [ns-form (read-string s)]
    ;; REVIEW: It would be possible to default to cljs.user or expanse.scratch
    ;; or something of the sort if no ns form is found. Is this desirable /
    ;; useful?
    (assert (= (first ns-form) 'ns))
    (second (read-string s))))

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
                              [".clj" ".cljc"]
                              [".cljs" ".cljc" ".js"])
                            cb)))

(defn eval [forms cb]
  (eval-soup/eval-forms
   (map eval-soup/wrap-macroexpand forms)
   cb
   eval-soup/state
   eval-ns
   load-fn))

(defn eval-str [s cb]
  (let [forms (read-all-forms s)]
    (eval forms cb)))
