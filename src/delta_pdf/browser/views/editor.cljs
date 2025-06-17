(ns delta-pdf.browser.views.editor
  (:require
   [re-frame.core :as rf]
   ["pdf-lib" :refer [PDFTextField
                      PDFCheckBox
                      PDFDropdown
                      PDFOptionList
                      PDFRadioGroup]]
   [delta-pdf.browser.events :as events]
   [delta-pdf.browser.subs :as subs]))

(defn- field->id [field script?]
  (str "delta-pdf-field-data-"
       (when script? "script-toggle-")
       (.getName field)))

(defmulti field-ed type)

(defmethod field-ed PDFTextField [field]
  [:span
   (let [nam (.getName field)
         attr {:id (field->id field false)
               :value @(rf/subscribe [::subs/field-body nam])
               :on-change #(rf/dispatch [::events/update-body
                                         nam
                                         (-> % .-target .-value)])}]
     (if (.isMultiline field)
       [:textarea attr]
       [:input (assoc attr :type "text")]))])

#_(defmethod field-ed PDFCheckBox [field]
  [:span
   [:input {:id (field->id field false)
            :type "checkbox"}]])

(defmethod field-ed :default [& args]
  [:span (str "no support for type " (.-name (type (first args))))])

(defn editor [field]
  (let [script-id (field->id (:obj field) true)
        id (field->id (:obj field) false)
        name (.getName (:obj field))]
    [:div
     [:label {:for script-id} "Script Mode: "]
     [:input {:type "checkbox"
              :id script-id
              :default-checked (:script? field)
              :on-change #(rf/dispatch [::events/toggle-script? name])}]
     [:div
      (if (:script? field)
        [:textarea {:id id
                    :on-change #(rf/dispatch [::events/update-body name (-> % .-target .-value)])
                    :value @(rf/subscribe [::subs/field-body name])}]
        (field-ed (:obj field)))]]))
