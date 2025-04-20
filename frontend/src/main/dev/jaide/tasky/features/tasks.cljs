(ns dev.jaide.tasky.features.tasks
  (:require
   [clojure.pprint :refer [pprint]]
   [dev.jaide.finity.core :as fsm]
   [dev.jaide.tasky.state.app-fsm :refer [app-fsm]]))

(defn th
  [& children]
  (into
   [:th.py-3.px-4.text-left]
   children))

(defn td
  [{:as attrs} & children]
  (into
   [:td.py-3.px-4
    (or attrs {})]
   children))

(defn task-row
  [{:keys [task tasks level]
    :or {level 0}}]
  (let [subtasks (->> tasks
                      (filter #(= (:parent_task_id %)
                                  (:id task))))]
    [:<>
     [:tr
      [td {:class "text-left"}
       [:span
        {:style {:paddingLeft (str level "rem")}}
        (:title task)]]
      [td {}
       (or (:due_date task) "-")]
      [td {}
       (:estimated_time task)]
      [td {}
       (:tracked_time task)]
      [td {}
       [:form
        [:input
         {:type "checkbox"
          :name "complete"
          :checked (some? (:completed_at task))}]]]]
     (for [task subtasks]
       [task-row
        {:key (:id task)
         :task task
         :tasks tasks
         :level (inc level)}])]))

(defn tasks-table
  [{:keys [tasks]}]
  (let [root-tasks (->> tasks
                        (filter #(nil? (:parent_task_id %))))]
    [:div.overflow-x-auto.shadow-md.rounded-lg
     [:table.min-w-full.table-auto
      [:thead
       {:class "bg-gray100 dark:bg-slate-600"}
       [:tr
        [th "Name"]
        [th "Due"]
        [th "Estimate"]
        [th "Elapsed"]
        [th ""]]]

      [:tbody
       (for [task root-tasks]
         [task-row {:key (:id task)
                    :task task
                    :tasks tasks}])]]]))

(defn new-task
  []
  (fsm/dispatch app-fsm {:type :new-task}))

(defn tasks-index
  [{:keys [tasks]}]
  [:secton
   {:id "tasks-container"
    :class "space-y-4"}
   [:header
    {:class "flex flex-row justify-between items-end"}
    [:h1
     {:class "text-2xl font-bold"}
     "Tasks"]
    [:div
     {:class "inline-flex flex-row gap-2 justify-end"}
     [:button
      {:type "button"
       :class "btn bg-blue-500"
       :on-click new-task}
      "New Task"]]]
   [tasks-table
    {:tasks tasks}]])
