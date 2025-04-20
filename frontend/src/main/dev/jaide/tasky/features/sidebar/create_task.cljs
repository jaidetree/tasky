(ns dev.jaide.tasky.features.sidebar.create-task)

(defn create-task
  [event])

(defn update-task
  [])

(defn field
  [{:keys [label id]} & children]
  (into
   [:section
    [:label.block.py-2
     {:htmlFor id}
     label]]
   children))

(defn title-field
  []
  [field
   {:id "id_title"
    :label "Title"}
   [:input {:id "id_title"
            :type :text
            :name :title
            :class "bg-stone-700/20 p-2 rounded-sm w-full"}]])

(defn notes-field
  []
  [field
   {:id "id_notes"
    :label "Notes"}
   [:textarea
    {:name "notes"
     :id "id_notes"
     :class "bg-stone-700/20 p-2 rounded-sm w-full h-40"}]])

(defn estimated-time-field
  []
  [field
   {:id "id_estimated_time"
    :label "Estimate"}
   [:div.flex.flex-row.gap-2.items-center.justify-evenly
    [:div.flex.flex-col.gap-2.items-center
     [:input
      {:type :range
       :name "estimated_time_hours"
       :min "0"
       :max "23"
       :step "1"
       :class "block bg-stone-700/20 p-2 rounded-sm flex-shrink w-full"}]
     [:span
      [:output
       (str " hrs")]]]
    [:div.flex.flex-col.gap-2.items-center
     [:input
      {:type :range
       :name "estimated_time_minutes"
       :min "0"
       :max "59"
       :class "block bg-stone-700/70 p-2 rounded-sm flex-shrink w-full"}]
     [:span
      [:output
       (str " mins")]]]]])

(defn due-date-field
  []
  [field
   {:id "id_due_date"
    :label "Due Date"}
   [:input
    {:id "id_due_date"
     :type "date"
     :class "bg-stone-700/20 p-2 rounded-sm w-full"}]])

(defn parent-task-field
  []
  [field
   {:id "id_parent_task_id"
    :label "Parent Task"}
   [:select
    {:id "id_parent_task_id"
     :class "bg-stone-700/20 p-2 rounded-sm w-full"
     :name "parent_task_id"}
    [:option {:value ""} "-- No Parent Task --"]
    (for [task []]
      [:option
       {:value (:id task) :key (:id task)}
       (:title task)])]])

(defn new-task-form
  []
  [:form.gap-2
   {:on-submit create-task
    :on-input update-task}
   [:header
    [:h2.text-xl "New Task"]]
   [:div.flex.flex-col.gap-2
    [title-field]
    [notes-field]
    [estimated-time-field]
    [due-date-field]
    [parent-task-field]
    [:section.flex.flex-row.justify-end.items-center.gap-2.py-4
     [:button
      {:type "submit"
       :class "btn py-2 px-4 text-white bg-blue-500"}
      "Create"]]]])

