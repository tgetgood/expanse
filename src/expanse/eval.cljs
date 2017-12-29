(ns expanse.eval
  (:require [cljs.core.async :refer [<! >! chan put!] :refer-macros [go]]
            [cljs.js :refer [empty-state eval js-eval]]
            [cljs.reader :refer [read-string]]
            [clojure.string :as string])
  (:import goog.net.XhrIo))

(set! cljs.js/*eval-fn* cljs.js/js-eval)

(comment
  "Dummy ns in which to eval things. Not sure what the point of this is...")
(defonce eval-ns
  (do (create-ns 'expanse.eval.evaluation)
      'expanse.eval.evaluation))

(def root-path "/js/compiled/out/")

(defn- with-root-path [opts]
  (update opts :path #(str root-path %)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Copied (and modified) eval-soup code
;;;;; https://github.com/oakes/eval-soup
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce state (empty-state))

(defn fix-goog-path [path]
  ; goog/string -> goog/string/string
  ; goog/string/StringBuffer -> goog/string/stringbuffer
  (let [parts     (string/split path #"/")
        last-part (last parts)
        new-parts (concat
                   (butlast parts)
                   (if (= last-part (string/lower-case last-part))
                     [last-part last-part]
                     [(string/lower-case last-part)]))]
    (string/join "/" new-parts)))

(defn custom-load!
  ([opts cb]
   (if (re-matches #"^goog/.*" (:path opts))
     (custom-load!
       (update opts :path fix-goog-path)
       [".js"]
       cb)
     (custom-load!
       opts
       (if (:macros opts)
         [".cljc" ".clj"]
         [ ".js" ".cljc" ".cljs"])
       cb)))
  ([opts extensions cb]
   (if-let [extension (first extensions)]
     (try
       (.send XhrIo
         (str (:path opts) extension)
         (fn [e]
           (if (.isSuccess (.-target e))
             (cb {:lang (if (= extension ".js") :js :clj)
                  :source (.. e -target getResponseText)})
             (custom-load! opts (rest extensions) cb))))
       (catch js/Error _
         (custom-load! opts (rest extensions) cb)))
     (cb {:lang :clj :source ""}))))


(defn eval-wrap [state form opts]
  (let [channel (chan)]
    (try
      (eval state form opts #(put! channel {:wrap (or (:error %) (:value %))}))
      (catch js/Error e (put! channel {:wrap e})))
    channel))

(defn eval-forms [forms state opts]
  (go
    (loop [forms   forms
           results []]
      (if (seq forms)
        (let [form (first forms)]
          (if (instance? js/Error form)
            (recur (rest forms) (conj results {:wrap form}))
            (recur (rest forms)
                   (conj results (<! (eval-wrap state form opts))))))
        (mapv :wrap results)))))

(defn wrap-macroexpand [form]
  (if (coll? form)
    (list 'macroexpand (list 'quote form))
    form))

(defn load-fn [opts cb]
  (custom-load! (with-root-path opts) cb))


(def default-opts
  {:eval          js-eval
   :load          load-fn
   :context       :expr
   :def-emits-var true})

(defn code->results
  ([forms]
   (code->results forms default-opts))
  ([forms opts]
   (let [out (chan)]
     (go
       (let [mac (map wrap-macroexpand forms)
             e1 (<! (eval-forms mac state opts))
             e2 (<! (eval-forms e1 state opts))]
         (>! out e2)))
     out)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; And Frog
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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

(defn eval-file-str [code cb]
  (let [ns (resolve-ns code)
        forms (read-all-forms code)]
    (go
      (cb (<! (code->results forms (assoc default-opts :ns ns)))))))

;; (def t (:code @expanse.core/app-state))
;; (def forms (read-all-forms t))
