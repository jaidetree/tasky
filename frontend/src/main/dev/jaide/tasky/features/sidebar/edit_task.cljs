(ns dev.jaide.tasky.features.sidebar.edit-task
  (:require
   [promesa.core :as p]
   [reagent.core :as r]
   [dev.jaide.finity.core :as fsm]
   [dev.jaide.valhalla.core :as v]
   [dev.jaide.tasky.router :as router]
   [dev.jaide.tasky.utils :refer [class-names]]
   [dev.jaide.tasky.tasks :refer [task-validator session-validator fetch-task]]
   [dev.jaide.tasky.state-machines :refer [ratom-fsm]]
   [dev.jaide.tasky.state.tasks-fsm :as tasks-fsm]))

(def estimates
  [["1 quarter" (* 3 30 24 60)]
   ["2 months" (* 2 30 24 60)]
   ["1 month" (* 30 24 60)]
   ["3 weeks" (* 3 7 24 60)]
   ["2 weeks" (* 2 7 24 60)]
   ["1 week" (* 7 24 60)]
   ["3 days" (* 3 24 60)]
   ["2 days" (* 2 24 60)]
   ["1 day" (* 24 60)]
   ["8 hr" (* 8 60)]
   ["6 hr" (* 6 60)]
   ["4 hr" (* 4 60)]
   ["3 hr" (* 3 60)]
   ["2 hr" (* 2 60)]
   ["1 hr" 60]
   ["45 min" 45]
   ["30 min" 30]
   ["20 min" 20]
   ["15 min" 15]
   ["10 min" 10]])

(defn- min->str
  [minutes]
  (let [minutes (js/Number minutes)]
    (loop [estimates estimates]
      (let [[[label estimate] & estimates] estimates]
        (cond
          (nil? estimate)
          "unknown"

          (= estimate minutes)
          label

          :else
          (recur estimates))))))

(defn field
  [{:keys [label id]} & children]
  (into
   [:section
    [:label.block.py-2
     {:htmlFor id}
     label]]
   children))

(defn title-field
  [{:keys [task-fsm]}]
  (let [title (get-in task-fsm [:task :title])]
    [field
     {:id "id_title"
      :label "Title"}
     [:input {:id "id_title"
              :type :text
              :name :title
              :value title
              :class "bg-stone-800 p-2 rounded-sm w-full"}]]))

(defn description-field
  [{:keys [task-fsm]}]
  (let [description (get-in task-fsm [:task :description])]
    [field
     {:id "id_description"
      :label "Description"}
     [:textarea
      {:name "description"
       :id "id_description"
       :class "bg-stone-800 p-2 rounded-sm w-full h-40"
       :value description}]]))

(defn estimated-time-field
  [{:keys [task-fsm]}]
  [field
   {:id "id_estimated_time"
    :label "Estimate"}
   [:select
    {:class "bg-stone-800 rounded-sm p-2 w-full"
     :name "estimated_time"
     :value (js/String (get-in task-fsm [:task :estimated_time]))}
    [:option
     {:value ""}
     "-- unknown --"]
    (for [[label min] estimates]
      [:option
       {:key min
        :value min}
       label])]])

(defn due-date-field
  [{:keys [task-fsm]}]
  (let [due-date (get-in task-fsm [:task :due_date])]
    [field
     {:id "id_due_date"
      :label "Due Date"}
     [:input
      {:id "id_due_date"
       :type "datetime-local"
       :value due-date
       :class "bg-stone-800 p-2 rounded-sm w-full"}]]))

(defn parent-task-field
  [{:keys [task-fsm]}]
  (let [parent-task-id (get-in task-fsm [:task :parent_task_id])
        selected-task-id (router/get-selected-task-id)
        tasks (->> (tasks-fsm/all-tasks)
                   (filter #(or (= (:id %) selected-task-id)
                                (= (:parent_task_id %) selected-task-id))))]
    [field
     {:id "id_parent_task_id"
      :label "Parent Task"}
     [:select
      {:id "id_parent_task_id"
       :class "bg-stone-800 p-2 rounded-sm w-full"
       :name "parent_task_id"
       :value parent-task-id}
      [:option {:value ""} "-- No Parent Task --"]
      (for [task tasks]
        [:option
         {:value (:id task) :key (:id task)}
         (:title task)])]]))

(defn update-task
  [event task-fsm]
  nil)

(defn submit-form
  [event task-fsm]
  nil)

(defn edit-task-form
  [{:keys [task-fsm]}]
  (let [props {:task-fsm task-fsm}]
    [:form.gap-2
     {:on-submit #(submit-form % task-fsm)
      :on-input #(update-task % task-fsm)}
     [:header
      [:h2.text-xl "Edit Task"]]
     [:div.flex.flex-col.gap-2
      [title-field
       props]
      [description-field
       props]
      [estimated-time-field
       props]
      [due-date-field
       props]
      [parent-task-field
       props]
      [:section.flex.flex-row.justify-end.items-center.gap-2.py-4
       [:button
        {:type "submit"
         :class "btn py-2 px-4 text-white bg-blue-500"}
        "Update"]]]]))

(defn task-view
  [{:keys [task-id]}]
  (let [task-fsm (tasks-fsm/find-task-fsm task-id)]
    [:div
     [edit-task-form
      {:task-fsm task-fsm}]]))

