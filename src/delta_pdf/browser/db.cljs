(ns delta-pdf.browser.db
  (:require
   [reagent.core :as r]))

(def default
  {:bytes 0
   :form nil
   :fields #{}})

(def app (r/atom default))
