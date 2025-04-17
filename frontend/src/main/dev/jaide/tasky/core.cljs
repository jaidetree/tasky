(ns dev.jaide.tasky.core
  (:require
   [reagent.core :as r]
   [reagent.dom.client :as rdomc]
   ["#src/app.css"]
   ["@heroicons/react/24/outline" :refer [BoltIcon]]))

(defonce root (rdomc/create-root (js/document.getElementById "root")))

(defn app
  []
  [:div
   [:> BoltIcon
    {:class "size-5"}]
   "It works!"])

(defn -main
  []
  (rdomc/render root [app]))

