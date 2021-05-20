(ns todo-reframe.core
  (:require
   [reagent.dom :as rdom]
   [re-frame.core :as rf :refer [dispatch dispatch-sync]]
   [todo-reframe.events]
   [todo-reframe.views :as views]
   [todo-reframe.config :as config]))


;; Enabling Console Log
(enable-console-print!)


;; Initializing DB
(dispatch-sync [:initialise-db])

(defn dev-setup []
  (when config/debug?
    (println "dev mode")))

(defn ^:dev/after-load mount-root []
  (rf/clear-subscription-cache!)
  (let [root-el (.getElementById js/document "app")]
    (rdom/unmount-component-at-node root-el)
    (rdom/render [views/main] root-el)))

(defn init []
  (dev-setup)
  (mount-root))
