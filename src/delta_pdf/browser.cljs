(ns delta-pdf.browser
  "Browser interface to ΔPDF."
  (:require
   [reagent.dom :as rdom]
   [re-frame.core :as rf]
   [delta-pdf.browser.views :as views]
   [delta-pdf.browser.events :as events]))

(defn- ^:dev/after-load mount-root
  "upon init and after reloading"
  []
  (rf/clear-subscription-cache!)
  (rf/dispatch-sync [::events/init-db])
  (let [root-el (.getElementById js/document "app")]
    (rdom/unmount-component-at-node root-el)
    (rdom/render [views/main] root-el)))

;; (defn ^:dev/before-load stop []
;;   ;; before reloading.
;;   (js/console.log "stop"))

(defn init []
  (mount-root))
