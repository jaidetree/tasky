(ns dev.jaide.tasky.core
  (:require
   [reagent.core :as r]
   [reagent.dom :as rdom]
   [dev.jaide.tasky.app :refer [app]]))

(defn -main
  []
  (rdom/render [app] (js/document.getElementById "root")))

