(ns dev.jaide.tasky.app
  (:require
   [reagent.core :as r]
   [dev.jaide.finity.core :as fsm]
   [dev.jaide.tasky.router :as router]
   [dev.jaide.tasky.state.tasks-fsm :refer [tasks-fsm]]
   [dev.jaide.tasky.features.tasks :refer [tasks-index]]
   [dev.jaide.tasky.features.task :refer [task-view]]
   [dev.jaide.tasky.features.toaster :refer [toaster]]
   [dev.jaide.tasky.views.diagrams :refer [diagrams]]))

(defn ^:dev/after-load fetch-tasks
  []
  (try
    (fsm/dispatch tasks-fsm :fetch)
    (catch :default err
      (js/console.error err))))

(defn app
  []
  (let [{:keys [state context]} @tasks-fsm]
    [:div
     [:header.px-12.py-8
      {:class "flex flex-row justify-between items-end px-8"}
      [:h1
       {:class "text-2xl font-bold"}
       "Tasky"]]
     [:div.flex.flex-row.items-stretch.min-h-screen
      (case state
        :tasks [:main.flex-grow.mx-auto.px-4.overflow-auto.max-w-full
                (let [routes (router/routes)]
                  (cond
                    (not= (get routes "tasks") "") [task-view]
                    :else [tasks-index {:tasks (:tasks context)}]))
                [:div.mt-20
                 [diagrams]]]
        [:div "Loading..."])]
     [toaster]]))

(fetch-tasks)
