(ns dev.jaide.tasky.features.task
  (:require
   [clojure.string :as s]
   [reagent.core :as r]
   [dev.jaide.finity.core :as fsm]
   [dev.jaide.tasky.dom :refer [timeout]]
   [dev.jaide.tasky.state.selectors :as select]
   [dev.jaide.tasky.views.task-form :as task-form]
   [dev.jaide.tasky.features.tasks :refer [breadcrumbs tasks-table]]))

(defn form-field
  [{:keys [id label class]} & children]
  (into
   [:div
    {:class class}
    [:label
     {:class "block text-sm py-1 text-slate-300"}
     label]]
   children))

(def timer-ref #js {:current nil})

(defn update-title
  [event]
  (let [value (-> event .-currentTarget .-value)
        clear-timeout (.-current timer-ref)
        task-fsm @select/selected-task-fsm]
    (when clear-timeout
      (clear-timeout))
    (.preventDefault event)
    (.stopPropagation event)
    (set! (.-current timer-ref)
          (timeout 1000 #(fsm/dispatch task-fsm {:type :update :data {[:title] value}})))))

(defn editable-title
  []
  (r/with-let [editing-ref (r/atom false)]
    (let [editing @editing-ref
          task-fsm @select/selected-task-fsm
          task (get task-fsm :task)]
      [:div.flex-grow
       {:on-click (when-not editing
                    #(reset! editing-ref true))
        :on-keydown (when editing
                      #(when (= (.. % -key) "Escape")
                         (.preventDefault %)
                         (reset! editing-ref false)))}
       (if editing
         [:input.px-2.py-1.text-2xl
          {:type "text"
           :class "bg-transparent border-0 w-full"
           :ref #(when %
                   (.focus %))
           :name "title"
           :default-value (:title task)
           :on-input update-title}]
         [:h2.text-2xl.px-2.py-1
          (:title task)])])
    (finally
      (when-let [timer (.-current timer-ref)]
        (js/clearTimeout timer)
        (set! (.-current timer-ref)
              nil)))))

(defn task-details
  [{:keys []}]
  (let [task-fsm @select/selected-task-fsm
        task (get task-fsm :task)]
    [:div.space-y-8
     [:section.flex.flex-row.gap-4.justify-between
      [:form.flex.flex-row.gap-2.items-center
       {:on-input #(task-form/update-task % task-fsm)}
       [task-form/checkbox
        {:checked (task-form/completed? task)}]
       [editable-title]]
      [:div.flex.flex-row.gap-4
       [:div
        "Elapsed"
        "/"
        "Total"]
       [:div
        "Donut Chart"]]]
     [:section
      [:form.flex.flex-row.gap-4
       {:on-change #(task-form/update-task % task-fsm)}
       [form-field
        {:id "task-estimate"
         :label "Estimate"}
        [task-form/estimate
         {:id "task-estimate"
          :value (:estimated_time task)}]]
       [form-field
        {:id "task-due-date"
         :label "Due Date"}
        [task-form/due-date
         {:value (task-form/date->string (:due_date task))}]]
       [form-field
        {:id "task-parent-task-id"
         :label "Parent Task"
         :class "flex-grow"}
        [task-form/parent-task
         {:id "task-parent-task-id"
          :value (:parent_task_id task)
          :tasks @select/all-tasks}]]]]
     [:section
      [:div.p-4.rounded-md.bg-zinc-800
       (let [description (:description task)]
         (if (s/blank? description)
           [:span.italic "No description"]
           description))]]]))

(defn task-view
  [{:keys []}]
  (let [task-id @select/selected-task-id]
    [:div.px-8.space-y-16
     [breadcrumbs
      {:task-id task-id}]
     [task-details
      {}]
     [tasks-table]]))
