(ns dev.jaide.tasky.app
  (:require
   [dev.jaide.tasky.features.sidebar :refer [sidebar]]
   [dev.jaide.tasky.state.tasks-fsm :refer [tasks-fsm]]
   [dev.jaide.tasky.features.tasks :refer [tasks-index]]))

(defn app
  []
  (let [{:keys [state context]} @tasks-fsm]
    [:div.flex.flex-row.items-stretch.min-h-screen
     (case state
       :tasks [:main.flex-grow.mx-auto.px-4.py-20.overflow-auto
               [tasks-index {:tasks (:tasks context)}]]
       [:div "Loading..."])
     [sidebar]]))
