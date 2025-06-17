(ns delta-pdf.browser.views
  (:require
   [re-frame.core :as rf]
   [delta-pdf.browser.events :as events]
   [delta-pdf.browser.subs :as subs]
   [delta-pdf.browser.views.editor :refer [editor]]))

(defn- parameters []
  [:div
   [:div
    [:label {:for "form"} "PDF Form: "]
    [:input {:id "form"
             :type "file"
             :accept "application/pdf"
             :on-change #(events/upload-pdf [::events/upload-form %])}]]
   [:div
    [:label {:for "table"} "Deltas: "]
    [:input {:id "table"
             :type "file"
             :accept ".tsv,.txt"
             :on-change #(events/upload-text [::events/upload-deltas %])}]]
   (when @(rf/subscribe [::subs/form?])
     [:div
      [:label {:for "import-template"} "Import Template: "]
      [:input {:id "import-template"
               :type "file"
               :accept ".edn"
               :on-change #(events/upload-text [::events/upload-template %])}]])
   (when @(rf/subscribe [::subs/form?])
     [:div
      [:button {:id "export-template"
                :on-click #(rf/dispatch [::events/export-template])}
       "Export Template"]
      [:button {:id "autofill-pdfs"
                :on-click #(rf/dispatch [::events/autofill-pdfs])}
       "Autofill ΔPDFs"]])])

(defn- fields-list []
  (when-let [fields @(rf/subscribe [::subs/template-fields])]
    [:div
     [:ul
      (for [f fields]
        (let [n (key f)]
          [:li {:key n}
           [:span
            [:button {:id "delete-field"
                      :value n
                      :on-click #(rf/dispatch [::events/remove-field n])}
             "🗑️"]
            n
            [editor (val f)]]]))]]))

(defn- dropdown []
  (when-let [fields @(rf/subscribe [::subs/field-names])]
    [:div
     [:label {:for "dropdown"} "Fields: "]
     [:select {:id "dropdown"}
      (for [f fields]
        [:option {:key f} f])]
     [:button {:id "add-field"
               :on-click #(rf/dispatch [::events/add-field (-> "dropdown" js/document.getElementById .-value)])} "+"]]))

(defn main []
  [:div
   [parameters]
   [dropdown]
   [fields-list]])
