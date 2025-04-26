(ns dev.jaide.tasky.app
  (:require
   [reagent.core :as r]
   [dev.jaide.finity.core :as fsm]
   [dev.jaide.tasky.features.sidebar :refer [sidebar]]
   [dev.jaide.tasky.state.tasks-fsm :refer [tasks-fsm]]
   [dev.jaide.tasky.features.tasks :refer [tasks-index]]
   [dev.jaide.tasky.views.diagrams :refer [diagrams]]))

(defn fetch-tasks
  []
  (try
    (fsm/dispatch tasks-fsm :fetch)
    (catch :default err
      (js/console.error err))))

(defn app
  []
  (r/with-let [_ (fetch-tasks)]
    (let [{:keys [state context]} @tasks-fsm]
      [:div
       [:div.flex.flex-row.items-stretch.min-h-screen
        (case state
          :tasks [:main.flex-grow.mx-auto.px-4.py-20.overflow-auto.max-w-full
                  [tasks-index {:tasks (:tasks context)}]]
          [:div "Loading..."])
        [sidebar]]
       [diagrams]])))
