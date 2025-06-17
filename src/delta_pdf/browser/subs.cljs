(ns delta-pdf.browser.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
 ::form?
 (fn [db]
   (boolean (:form db))))

(rf/reg-sub
 ::field-names
 (fn [db]
   (when-let [form (:form db)]
     (map #(.getName %) (.getFields form)))))

(rf/reg-sub
 ::template-fields
 (fn [db]
   (:fields db)))

(rf/reg-sub
 ::field-body
 (fn [db [_ name]]
   (get-in db [:fields name :body])))
