(ns delta-pdf.browser.events
  (:require
   [clojure.set :as r]
   [cljs.pprint :refer [pprint]]
   [cljs.core.async :refer [<!] :refer-macros [go]]
   [cljs.core.async.interop :refer-macros [<p!]]
   [re-frame.core :as rf]
   [delta-pdf.core :refer [autofill field->type]]
   [delta-pdf.browser.db :as db]
   [goog.labs.format.csv :as csv]
   [clojure.edn :as edn]
   ["pdf-lib" :refer [PDFDocument]]))

(rf/reg-event-db
 ::init-db
 (fn [_db _]
   _db))

(defn upload-pdf
  "Handle FileReader ceremony for uploading pdfs."
  [[dispatch event]]
  (let [input (first (-> event .-target .-files))
        reader (js/Promise.
                (fn [res rej]
                  (let [fr (js/FileReader.)]
                    (set! (.-onload fr) #(res (.-result fr)))
                    (set! (.-onerror fr) #(rej %))
                    (.readAsArrayBuffer fr input))))]
    (go
      (try
        (let [bytes (<p! reader)
              pdf (<p! (.load PDFDocument bytes))]
          (rf/dispatch [dispatch pdf bytes]))
        (catch :default e
          (js/alert "An error occured while reading PDF.")
          (js/console.error e))))))

(rf/reg-event-db
 ::upload-form
 (fn [db [_ pdf bytes]]
   (let [form (.getForm pdf)]
     (assoc db
            :bytes bytes
            :pdf pdf
            :form form
            :fields {}))))

(defn upload-text
  [[dispatch event]]
  (let [input (first (-> event .-target .-files))
        reader (js/Promise.
                (fn [res rej]
                  (let [fr (js/FileReader.)]
                    (set! (.-onload fr) #(res (.-result fr)))
                    (set! (.-onerror fr) #(rej %))
                    (.readAsText fr input))))]
    (go
      (try
        (let [txt (<p! reader)]
          (rf/dispatch [dispatch txt]))
        (catch :default e
          (js/alert "An error occured while reading deltas.")
          (js/console.error e))))))

(rf/reg-event-db
 ::upload-deltas
 (fn [db [_ txt]]
   (let [tsv (csv/parse txt false "\t")]
     (assoc db :deltas (js->clj tsv)))))

(rf/reg-event-db
 ::upload-template
 (fn [db [_ template]]
   (let [form (:form db)]
     (->> (for [f (edn/read-string template)]
            (if-let [field (.getField form (key f))]
              [(key f)
               {:obj field
                :script? (= :script (:type (val f)))
                :type (field->type field)
                :body (:body (val f))}]
              nil))
          (filter identity)
          (into {})
          (assoc db :fields)
          prn-str
          js/console.log)
     (->> (for [f (edn/read-string template)]
            (if-let [field (.getField form (key f))]
              [(key f)
               {:obj field
                :script? (= :script (:type (val f)))
                :type (field->type field)
                :body (:body (val f))}]
              nil))
          (filter identity)
          (into {})
          (assoc db :fields))
     )))

(rf/reg-event-db
 ::add-field
 (fn [db [_ field-name]]
   (when-not (some #(= (key %) field-name) (:fields db))
     (let [f (.getField (:form db) field-name)
           t (field->type f)]
       (update db :fields conj
               {field-name {:obj f
                            :script? false
                            :type t
                            :body (case t
                                    :options []
                                    :text ""
                                    nil)}})))))

(rf/reg-event-db
 ::remove-field
 (fn [db [_ field-name]]
   (update db :fields dissoc field-name)))

(rf/reg-event-db
 ::toggle-script?
 (fn [db [_ field-name]]
   (update-in db [:fields field-name :script?] not)))

(rf/reg-event-db
 ::update-body
 (fn [db [_ field-name nu]]
   (assoc-in db [:fields field-name :body] nu)))

(defn download-file [filename content type]
  (let [a (js/document.createElement "a")
        url (js/URL.createObjectURL (js/Blob. #js [content] #js {:type type}))]
    (js/console.dir url)
    (.setAttribute a "href" url)
    (.setAttribute a "download" filename)
    (set! (.-display (.-style a)) "none")
    (js/document.body.appendChild a)
    (.click a)
    (js/document.body.removeChild a)))

(defn generate-template [fields]
  (->> (vals fields)
       (map #(if (:script? %) (assoc % :type :script) %))
       (map #(select-keys % [:type :body]))
       (zipmap (keys fields))))

(rf/reg-event-db
 ::export-template
 (fn [db [_]]
   (let [fields (:fields db)]
     (download-file
      "template.edn"
      (with-out-str (pprint (generate-template fields)))
      "plain/text"))
   db))

(defn- fill-pdfs [db]
  (let [pdf (:pdf db)
        buf-pdf (:bytes db)
        deltas (:deltas db)
        template (generate-template (:fields db))]
    (go (doseq [out (autofill pdf buf-pdf deltas template)]
          (download-file "output.pdf" (<! out) "application/pdf")))))

(rf/reg-event-db
 ::autofill-pdfs
 (fn [db [_]]
   (fill-pdfs db)
   db))
