(ns dev.jaide.tasky.features.sidebar
  (:require
   [dev.jaide.tasky.utils :refer [class-names]]
   [dev.jaide.tasky.router :as router]
   [dev.jaide.tasky.features.sidebar.create-task :refer [new-task-form]]
   [dev.jaide.tasky.features.sidebar.edit-task :refer [task-view]]))

(defn sidebar-active?
  [routes]
  (or (contains? routes "task")
      (contains? routes "new")))

(defn routes->state
  [routes]
  (cond
    (contains? routes "new")  [:new-task nil]
    (contains? routes "task") [:view-task (get routes :task)]
    :else [:closed]))

(defn sidebar
  []
  (let [routes (router/routes)
        [state & args] (routes->state routes)]
    [:div
     {:class (class-names
              "bg-gray-200 dark:bg-slate-700/20 overflow-hidden transition-all duration-500"
              (if (sidebar-active? routes) "w-[30rem]" "w-0"))}
     [:div
      {:class "w-[30rem] p-4"}
      (case state
        :closed nil
        :new-task [new-task-form {}]
        :view-task (let [[id] args]
                     [task-view {:task-id id}]))]]))
