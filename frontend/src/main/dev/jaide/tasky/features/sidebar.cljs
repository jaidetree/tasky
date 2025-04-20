(ns dev.jaide.tasky.features.sidebar
  (:require
   [dev.jaide.tasky.utils :refer [class-names]]
   [dev.jaide.tasky.state.app-fsm :refer [app-fsm]]
   [dev.jaide.tasky.features.sidebar.create-task :refer [new-task-form]]
   [dev.jaide.tasky.features.sidebar.edit-task :refer [task-view]]))

(defn sidebar
  []
  (let [state (get app-fsm :state)
        is-active (not= state :closed)]
    [:div
     {:class (class-names
              "bg-gray-200 dark:bg-slate-700/20 overflow-hidden transition-all duration-500"
              (if is-active "w-[30rem]" "w-0"))}
     [:div
      {:class "w-[30rem] p-4"}
      (case state
        :closed nil
        :new-task [new-task-form {}]
        :view-task [task-view {}])]]))
