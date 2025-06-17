(ns delta-pdf.core
  "Automated PDF form filler with Δ."
  (:require
   [clojure.string :as str]
   [clojure.set :as set]
   [cljs.core.async :refer-macros [go]]
   [cljs.core.async.interop :refer-macros [<p!]]
   [goog.labs.format.csv :as csv]
   ["pdf-lib" :refer [PDFDocument
                      PDFForm
                      PDFField

                      PDFTextField
                      PDFCheckBox
                      PDFDropdown
                      PDFOptionList
                      PDFRadioGroup]]
   ["@ardatan/string-interpolation" :refer [Interpolator]]))

;; delta := a row in a table (csv/tsv) that represents a change in a pdf

(comment ;;; SCHEMA
  root
  {"field name" (or text options script)}

  text ; Text Field
  {:type :text
   :body "Foo: {delta}"}

  options ; Boolean type shit
  {:type :option
   :body ["options..."]}

  script
  {:type :script ; js only womp womp
   :body "(field, delta) => field.setText('foo')"})

(defn form->map [^PDFForm form]
  (let [fields (.getFields form)]
    (apply conj {}
           (for [^PDFField f fields]
             [(.getName f) f]))))

(def ^:private interpolator (Interpolator.))

(defn- compile-template
  "Compile template into a function."
  [[^PDFField field-type template]]
  (let [body (:body template)
        templ (case (:type template)
                :text
                (fn [field delta]
                  (do (.setText ^PDFTextField field (.parse interpolator body (clj->js delta)))
                      nil))

                :options
                (condp = (type field-type)
                  PDFCheckBox
                  (fn [^PDFCheckBox field _delta]
                    (when (contains? (set body) (.getName field))
                      (.check field)))

                  PDFOptionList
                  (fn [^PDFOptionList field _delta]
                    (.select field (clj->js body)))

                  PDFDropdown
                  (fn [^PDFDropdown field _delta]
                    (.select field (clj->js body)))

                  PDFRadioGroup
                  (fn [^PDFRadioGroup field _delta]
                    (.select field (some #(contains? (set body) %) (.getOptions field))))

                  :else (throw (js/Error. "This type of field is not supported.")))
                :script (js/eval body)

                (throw (js/Error. (str "This :type of template " (:type template) " is not supported."))))]
    [(.getName field-type) templ]))

(defn field->type [^PDFField f]
  (let [typ (type f)]
    (cond
      (= typ PDFTextField) :text
      (contains? #{PDFCheckBox PDFDropdown PDFOptionList PDFRadioGroup} typ) :options
      :else nil)))

(defn- create-default-template [^PDFField f]
  (let [nam (.getName f)]
    (case (field->type f)
      :text
      [nam {:type :text
            :body (str "{" nam "}")}]
      :options
      [nam {:type :options
            :body [nam]}]
      nil)))

(defn- apply-delta!
  "Fill out FORM using DELTA formatted by TEMPLATE"
  [^PDFDocument pdf delta template]
  (let [fields (form->map (.getForm pdf))]
    (doseq [[k fun] template]
      (fun (get fields k) delta))))

(defn autofill
  "Map DELTAS onto PDFs formatted by TEMPLATE."
  [^PDFDocument pdf buf-pdf deltas template]
  (let [deltas (js->clj deltas)
        d-heads (set (first deltas))
        fields ^Array (.getFields (.getForm pdf))
        implied-template
        (->> fields
             (filter #(contains? d-heads (.getName ^PDFField %)))
             (map create-default-template)
             (filter second)
             (into {}))
        templ
        (->> fields
             (zipmap (map #(.getName ^PDFField %) fields))
             (set/rename-keys (merge implied-template template))
             (map compile-template)
             (into {}))]
    (for [d (rest deltas)]
      (go
        (let [new-pdf (<p! (.load PDFDocument buf-pdf))]
          (apply-delta! new-pdf (zipmap d-heads d) templ)
          (<p! (.save new-pdf)))))))
