(ns dev.jaide.tasky.features.task
  (:require
   [clojure.string :as s]
   [reagent.core :as r]
   [dev.jaide.finity.core :as fsm]
   [dev.jaide.valhalla.core :as v]
   [dev.jaide.tasky.dom :refer [timeout]]
   [dev.jaide.tasky.state-machines :refer [ratom-fsm]]
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

(def submit-update-fsm-spec
  (fsm/define
    {:id :debounce-update-fsm

     :initial {:state :display
               :context {}}

     :states {:display {}
              :editing {:value (v/string)}}

     :actions {:edit {:value (v/string)}
               :update {:value (v/string)}
               :cancel {}
               :done {}}

     :effects {}

     :transitions
     [{:from [:display]
       :actions [:edit]
       :to [:editing]
       :do (fn [state action]
             (let [value (:value action)]
               {:state :editing
                :context {:value value}}))}

      {:from [:editing]
       :actions [:update]
       :to [:editing]
       :do (fn [state action]
             (assoc-in state [:context :value] (:value action)))}

      {:from [:editing]
       :actions [:cancel :done]
       :to :display}]}))

(defn editable-title
  []
  (r/with-let [fsm (ratom-fsm submit-update-fsm-spec)]
    (let [{:keys [state context]} @fsm
          task-fsm @select/selected-task-fsm
          task (get task-fsm :task)
          class "flex-grow"]
      (if (= state :display)
        [:div
         {:class class
          :on-click #(fsm/dispatch fsm {:type :edit :value (:title task)})}
         [:h2.text-2xl.px-2.py-1
          (:title task)]]
        [:form
         {:class class
          :on-submit #(do
                        (.preventDefault %)
                        (fsm/dispatch task-fsm {:type :update :data {[:title] (:value context)}})
                        (fsm/dispatch fsm {:type :done}))
          :on-keydown #(when (= (.. % -key) "Escape")
                         (.preventDefault %)
                         (fsm/dispatch fsm :cancel))}
         [:input.px-2.py-1.text-2xl
          {:type "text"
           :class "bg-transparent border-0 w-full"
           :ref #(when %
                   (.focus %))
           :name "title"
           :value (:value context)
           :on-blur #(fsm/dispatch fsm {:type :cancel})
           :on-input #(let [value (.. % -currentTarget -value)]
                        (fsm/dispatch fsm {:type :update :value value}))}]]))
    (finally
      (fsm/destroy fsm))))

(defn editable-description
  []
  (r/with-let [fsm (ratom-fsm submit-update-fsm-spec)]
    (let [{:keys [state context]} @fsm
          task-fsm @select/selected-task-fsm
          {:keys [description]} (get task-fsm :task)
          class ""]
      (if (= state :display)
        [:div
         {:class class
          :on-click #(fsm/dispatch fsm {:type :edit :value description})}
         (if (s/blank? description)
           [:span.italic "No description"]
           description)]
        [:form
         {:class class
          :on-submit #(do
                        (.preventDefault %)
                        (fsm/dispatch task-fsm {:type :update :data {[:description] (:value context)}})
                        (fsm/dispatch fsm {:type :done}))
          :on-keydown #(do
                         (cond
                           (and (= (.. % -key) "Enter")
                                (or (.-ctrlKey %) (.-metaKey %)))
                           (do
                             (.preventDefault %)
                             (-> % (.-currentTarget) (.requestSubmit)))

                           (= (.. % -key) "Escape")
                           (do
                             (.preventDefault %)
                             (fsm/dispatch fsm :cancel))))}
         [:textarea
          {:class "bg-transparent border-0 w-full"
           :ref #(when %
                   (.focus %))
           :name "description"
           :value (:value context)
           :on-blur #(fsm/dispatch fsm {:type :cancel})
           :on-input #(let [value (.. % -currentTarget -value)]
                        (fsm/dispatch fsm {:type :update :value value}))}]]))
    (finally
      (fsm/destroy fsm))))

(defn task-details
  [{:keys []}]
  (let [task-fsm @select/selected-task-fsm
        task (get task-fsm :task)]
    [:div.space-y-8
     [:section.flex.flex-row.gap-4.justify-between
      [:div.flex.flex-row.gap-2.items-center
       [:form
        {:action "#"
         :on-input #(task-form/update-task % task-fsm)}
        [task-form/checkbox
         {:checked (task-form/completed? task)}]]
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
       [editable-description]]]]))

(defn task-view
  [{:keys []}]
  (let [task-id @select/selected-task-id]
    [:div.px-8.space-y-16
     [breadcrumbs
      {:task-id task-id}]
     [task-details
      {}]
     [tasks-table]]))
