(ns dev.jaide.tasky.features.sidebar
  (:require
   [dev.jaide.tasky.utils :refer [class-names]]
   [dev.jaide.tasky.router :as router]
   [dev.jaide.tasky.features.sidebar.create-task :refer [new-task-form]]
   [dev.jaide.tasky.features.sidebar.edit-task :refer [task-view]]
   ["@heroicons/react/24/outline" :refer [XMarkIcon]]))

(defn sidebar-active?
  [routes]
  (some? (:sidebar routes)))

(defn routes->state
  [{:keys [sidebar]}]
  (cond
    (contains? sidebar "new")  [:new-task nil]
    (get sidebar "task") [:view-task (get sidebar "task")]
    :else [:closed]))

(defn sidebar
  []
  (let [routes (router/routes)
        [state & args] (routes->state routes)]
    [:div
     {:class (class-names
              "bg-gray-200 dark:bg-black/40 overflow-hidden transition-all duration-500"
              (if (sidebar-active? routes) "w-[40rem]" "w-0"))}
     [:div
      {:class "w-full p-8 relative"}
      (when (not= state :closed)
        [:button
         {:class "rounded-full p-2 bg-black text-white border border-slate-600 absolute right-4"
          :on-click #(router/navigate nil :replace :sidebar)}
         [:> XMarkIcon
          {:class "size-4"}]])
      (case state
        :closed nil
        :new-task [new-task-form {}]
        :view-task (let [[id] args]
                     [task-view {:task-id id}]))]]))
