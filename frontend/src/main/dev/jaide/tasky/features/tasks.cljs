(ns dev.jaide.tasky.features.tasks
  (:require
   [dev.jaide.finity.core :as fsm]
   [dev.jaide.tasky.state-machines :refer [ratom-fsm]]
   [dev.jaide.tasky.state.app-fsm :refer [app-fsm]]
   [dev.jaide.tasky.tasks :as tasks]
   [dev.jaide.tasky.views.delete-rocker :refer [delete-rocker]]
   [dev.jaide.valhalla.core :as v]
   [promesa.core :as p]
   [reagent.core :refer [with-let]]))

(def error-validator
  (v/instance js/Error))

(defn timeout
  [ms f]
  (let [timer (js/setTimeout f ms)]
    (fn dispose
      []
      (js/clearTimeout timer))))

(def task-fsm-spec
  (fsm/define
    {:id :task-row

     :initial {:state :empty
               :context {}}

     :states {:empty {}
              :ready {:task tasks/task-validator
                      :error (v/nilable error-validator)}
              :saving {:task tasks/task-validator}
              :deleting {:task tasks/task-validator}
              :deleted {}}

     :actions {:init {:task tasks/task-validator}
               :update {:data (v/hash-map
                               (v/vector (v/keyword))
                               (v/union
                                (v/string)
                                (v/number)
                                (v/boolean)))}
               :save {}
               :updated {:task tasks/task-validator}
               :error {:error error-validator}
               :delete {}
               :deleted {}}

     :effects {:debounce [{:delay (v/number)
                           :timestamp (v/number)
                           :action (v/keyword)}
                          (fn [{:keys [dispatch effect]}]
                            (let [ms (get effect :delay)
                                  action (get effect :action)]
                              (timeout
                               #(dispatch {:type action})
                               ms)))]

               :save [{:timestamp (v/number)}
                      (fn [{{{:keys [task]} :context} :state :keys [dispatch _effect]}]
                        (let [abort-controller (js/AbortController.)]
                          (-> (tasks/update-task task (.-signal abort-controller))
                              (p/then #(dispatch {:type :updated :task %}))
                              (p/catch #(dispatch {:type :error :error %})))
                          (fn []
                            (.abort abort-controller))))]

               :delete [{:task tasks/task-validator}
                        (fn [{:keys [dispatch effect]}]
                          (let [task (:task effect)]
                            #_(-> (tasks/delete-task (:id task))
                                  (p/catch #(dispatch {:type :error :error %})))
                            (timeout
                             500
                             #(dispatch {:type :deleted}))))]}

     :transitions
     [{:from [:empty]
       :actions [:init]
       :to [:ready]
       :do (fn [state action]
             {:state :ready
              :context {:task (:task action)}})}

      {:from [:ready]
       :actions [:update]
       :to [:ready]
       :do (fn [{:keys [context]} {:keys [data]}]
             (let [task (:task context)]
               {:state :ready
                :context {:error (:error context)
                          :task (->> data
                                     (reduce
                                      (fn [task [path value]]
                                        (assoc-in task path value))
                                      task))}
                :effect {:id :debounce
                         :timestamp (js/Date.now)
                         :delay 500
                         :action :save}}))}

      {:from [:ready]
       :actions [:save]
       :to [:saving]
       :do (fn [state _action]
             {:state :saving
              :context {:task (get-in state [:context :task])}
              :effect {:id :save
                       :timestamp (js/Date.now)}})}

      {:from [:saving]
       :actions [:updated]
       :to [:ready]
       :do (fn [_state action]
             {:state :ready
              :context {:task (:task action)
                        :error nil}})}

      {:from [:saving :deleting]
       :actions [:error]
       :to [:ready]
       :do (fn [state action]
             {:state :ready
              :context {:task (get-in state [:context :task])
                        :error (get :error action)}})}

      {:from [:ready]
       :actions [:delete]
       :to [:deleting]
       :do (fn [state _action]
             {:state :deleting
              :context {:task (get-in state [:context :task])}
              :effect {:id :delete :task (get-in state [:context :task])}})}

      {:from [:deleting]
       :actions [:deleted]
       :to :deleted}]}))

(defn th
  [& children]
  (into
   [:th.py-3.px-4.text-left]
   children))

(defn td
  [{:as attrs} & children]
  (into
   [:td.py-2.px-4
    (or attrs {})]
   children))

(defn update-task
  [task-fsm event]
  (let [name (-> event (.-target) (.-name) (keyword))
        value (-> event (.-target) (.-value))
        data (case name
               :estimated_time_minutes {[:estimated_time_map :minutes] (js/Number value)}
               :estimated_time_hours   {[:estimated_time_map :minutes] (js/Number value)}
               {[name] value})]
    (fsm/dispatch task-fsm {:type :update :data data})))

(defn task-row
  [{:keys [task tasks level]
    :or {level 0}}]
  (with-let [task-fsm (ratom-fsm task-fsm-spec
                                 {:initial {:state :empty}})
             _ (fsm/dispatch task-fsm {:type :init :task task})]
    (let [subtasks (->> tasks
                        (filter #(= (:parent_task_id %)
                                    (:id task))))
          task (get task-fsm :task)
          form-id (str "task-form-" (:id task))]
      [:<>
       [:tr
        [td {:class "text-left"}
         [:div
          {:style {:paddingLeft (str level "rem")}}
          [:form.flex.flex-row.flex-nowrap.gap-2
           {:id form-id
            :on-input #(update-task task-fsm %)}
           [:input
            {:type "checkbox"
             :name "complete"
             :checked (some? (:completed_at task))}]
           [:input.flex-grow
            {:name "title"
             :value (:title task)}]]]]
        [td {}
         (or (:due_date task) "-")]
        [td {}
         (:estimated_time task)]
        [td {}
         (:tracked_time task)]
        [td {}
         [delete-rocker
          {:id (str "task-" (:id task))
           :on-delete #(fsm/dispatch task-fsm {:type :delete})}]]]

       (for [task subtasks]
         [task-row
          {:key (:id task)
           :task task
           :tasks tasks
           :level (inc level)}])])))

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
