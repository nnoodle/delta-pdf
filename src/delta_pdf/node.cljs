(ns delta-pdf.node
  "CLI/Node interface to ΔPDF."
  (:require
   [cljs.core.async :refer [<!] :refer-macros [go]]
   [cljs.core.async.interop :refer-macros [<p!]]
   [clojure.edn :as edn]
   [goog.labs.format.csv :as csv]
   ["node:fs/promises" :as fs]
   ["node:util" :refer [parseArgs]]
   ["pdf-lib" :refer [PDFDocument]]
   [delta-pdf.core :refer [autofill]]))

(def test-pdf)

(defn- load-pdf [link]
  (go (set! test-pdf (<p! (.load PDFDocument (<p! (fs/readFile link)))))))

(defn main [& cli-args]
  (let [args (parseArgs
              #js{:args (to-array cli-args)
                  :options #js{"help" #js{:type "boolean"
                                          :short "h"}
                               "delimiter " #js{:type "string"
                                                :short "d"
                                                :default "\t"}}
                  :allowPositionals true})
        pos (.-positionals args)]
    (if (or (.-help args) (not (< 1 (count pos) 4)))
      (js/console.error "usage: form.pdf deltas.tsv [template.edn?] [-h]")
      (let [template-path (when (= 3 (count pos)) (last pos))
            pdf-path (first pos)
            deltas-path (second pos)]
        (go
          (try
            (let [buf-pdf-p (fs/readFile pdf-path)
                  buf-delta-p (fs/readFile deltas-path #js{:encoding "utf8"})
                  templ (if template-path
                          (edn/read-string (<p! (fs/readFile template-path #js{:encoding "utf8"})))
                          {})
                  pdf-p (.load PDFDocument (<p! buf-pdf-p))
                  delta (csv/parse (<p! buf-delta-p) false "\t")]
              (loop [p (autofill (<p! pdf-p) (<p! buf-pdf-p) delta templ)
                     i 1]
                (when (seq p)
                  (fs/writeFile (str "new(" i ").pdf") (<! (first p)))
                  (recur (rest p) (inc i)))))
            (catch :default e
              (js/console.error (str "main: An error occured while processing files:\n" e)))))))))
