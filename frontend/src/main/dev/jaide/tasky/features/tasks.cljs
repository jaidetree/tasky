(ns dev.jaide.tasky.features.tasks
  (:require
   [dev.jaide.finity.core :as fsm]
   [dev.jaide.tasky.dom :refer [timeout]]
   [dev.jaide.tasky.state-machines :refer [ratom-fsm]]
   [dev.jaide.tasky.state.app-fsm :refer [app-fsm]]
   [dev.jaide.tasky.state.tasks-fsm :refer [tasks-fsm]]
   [dev.jaide.tasky.tasks :as tasks]
   [dev.jaide.tasky.views.delete-rocker :refer [delete-rocker]]
   [dev.jaide.valhalla.core :as v]
   [promesa.core :as p]
   [reagent.core :refer [class-names]]
   [reagent.core :refer [with-let]]))

(def error-validator
  (v/instance js/Error))

(def blank-task
  {:id ""
   :title ""
   :notes ""
   :estimated_time 20
   :estimated_time_map {:hours 0
                        :minutes 20}
   :due_date nil
   :tracked_time 0
   :parent_task_id ""
   :created_at (new js/Date)
   :updated_at (new js/Date)
   :completed_at nil
   :time_sessions []})

(def task-fsm-spec
  (fsm/define
    {:id :task-row

     :initial {:state :empty
               :context {:task blank-task}}

     :states {:empty {:task (v/nilable tasks/task-validator)}
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
               :created {:task tasks/task-validator}
               :error {:error error-validator}
               :reset {}
               :delete {}
               :deleted {}}

     :effects {:debounce [{:delay (v/number)
                           :timestamp (v/number)
                           :action (v/keyword)}
                          (fn [{:keys [dispatch effect]}]
                            (let [ms (get effect :delay)
                                  action (get effect :action)]
                              (timeout
                               ms
                               #(dispatch {:type action}))))]

               :update [{:timestamp (v/number)}
                        (fn [{{{:keys [task]} :context} :state :keys [dispatch _effect]}]
                          (let [abort-controller (js/AbortController.)]
                            (-> (tasks/update-task task (.-signal abort-controller))
                                (p/then #(dispatch {:type :updated :task %}))
                                (p/catch #(dispatch {:type :error :error %})))
                            (fn []
                              (.abort abort-controller))))]

               :create [{:timestamp (v/number)}
                        (fn [{{{:keys [task]} :context} :state :keys [dispatch _effect]}]
                          (let [abort-controller (js/AbortController.)]
                            (-> (tasks/create-task task (.-signal abort-controller))
                                (p/then #(dispatch {:type :created :task %}))
                                (p/catch #(dispatch {:type :error :error %})))
                            (fn []
                              (.abort abort-controller))))]

               :delete [{:task tasks/task-validator}
                        (fn [{:keys [dispatch effect]}]
                          (let [task (:task effect)]
                            (-> (tasks/delete-task (:id task))
                                (p/catch #(dispatch {:type :error :error %})))
                            (timeout
                             1000
                             #(dispatch {:type :deleted}))))]}

     :transitions
     [{:from [:empty]
       :actions [:init]
       :to [:ready]
       :do (fn [state action]
             {:state :ready
              :context {:task (:task action)}})}

      {:from [:ready :empty]
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
             (let [task (get-in state [:context :task])]
               {:state :saving
                :context {:task task}
                :effect {:id (if (= (:id task) "")
                               :create
                               :update)
                         :timestamp (js/Date.now)}}))}

      {:from [:saving]
       :actions [:reset :created]
       :to [:empty]
       :do (fn [_state _actions]
             {:state :empty
              :context {:task blank-task}})}

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
  (println event)
  (let [name (-> event (.-target) (.-name) (keyword))
        value (-> event (.-target) (.-value))
        data (case name
               :estimated_time_minutes
               {[:estimated_time_map :minutes] (js/Number value)}

               :estimated_time_hours
               {[:estimated_time_map :minutes] (js/Number value)}

               :estimated_time
               (let [value (js/Number value)
                     hours (js/Math.floor (/ value 60))
                     minutes (- value (* hours 60))]
                 {[:estimated_time] value
                  [:estimated_time_map :hours] hours
                  [:estimated_time_map :minutes] minutes})

               :complete {[:completed_at] (new js/Date)}
               {[name] value})]
    (cljs.pprint/pprint data)
    (fsm/dispatch task-fsm {:type :update :data data})))

(defn title
  [{:keys [value]}]
  [:input.flex-grow
   {:name "title"
    :class "text-sm"
    :value value}])

(defn checkbox
  [{:keys [checked]}]
  [:input
   {:type "checkbox"
    :name "complete"
    :checked checked}])

(defn due-date
  [{:keys [form-id value]}]
  [:input
   {:form form-id
    :class "text-xs"
    :type "datetime-local"
    :name "due_date"
    :value value}])

(defn- min->string
  [minutes]
  (let [hours (js/Math.floor (/ minutes 60))
        minutes (- minutes (* hours 60))
        s (if (pos? hours) (str hours " hrs") "")
        s (if (pos? minutes) (str s ", " minutes " min") s)]
    s))

(defn estimated-time
  [{:keys [form-id value]}]
  [:select
   {:form form-id
    :class "text-sm"
    :name "estimated_time"
    :value value}
   [:option
    {:value ""}
    "-- No Estimate --"]
   (for [min [10 15 20 30 45 60]]
     [:option
      {:key min
       :value min}
      (str min " min")])
   (for [min (range 60 (* 9 60) 60)]
     [:option
      {:key [min]
       :value min}
      (min->string min)])])

(def debug-atom
  (atom nil))

(defn task-row
  [{:keys [task tasks level]
    :or {level 0}}]
  (with-let [task-fsm (ratom-fsm task-fsm-spec
                                 {:initial {:state :empty}})
             _ (reset! debug-atom task-fsm)
             _ (fsm/subscribe task-fsm
                              (fn [{:keys [prev next action]}]
                                (when (= (:type action) :created)
                                  (println "refreshing tasks")
                                  (fsm/dispatch tasks-fsm {:type :refresh}))))

             _ (fsm/dispatch task-fsm {:type :init :task task})]
    (let [subtasks (->> tasks
                        (filter #(= (:parent_task_id %)
                                    (:id task))))
          task (get task-fsm :task)
          form-id (str "task-form-" (:id task))]
      [:<>
       (when (not= (:state @task-fsm) :deleted)
         [:tr
          {:class (class-names
                   "transition transition-opacity duration-500 ease-in-out"
                   (if (= (:state @task-fsm) :deleting)
                     "bg-red-500/25 opacity-0"
                     "opacity-100"))
           :on-change #(update-task task-fsm %)}

          [td {:class "text-left"}
           [:div
            {:style {:paddingLeft (str level "rem")}}
            [:form.flex.flex-row.flex-nowrap.gap-2
             {:id form-id
              :on-submit #(do (.preventDefault %)
                              (fsm/dispatch task-fsm {:type :save}))
              :on-input #(update-task task-fsm %)}
             [checkbox
              {:checked (some? (:completed_at task))}]
             [title
              {:value (:title task)}]]]]
          [td {}
           [due-date
            {:form-id form-id
             :value (if-let [date (:due_date task)]
                      (.toISOString date)
                      "")}]]
          [td {}
           [estimated-time
            {:form-id form-id
             :value (:estimated_time task)}]]
          [td {:class "text-sm"}
           (:tracked_time task)]
          [td {}
           [:div
            {:class "flex flex-row items-end"}
            (when-not (= (:id task) "")
              [delete-rocker
               {:id (str "task-" (:id task))
                :on-delete #(fsm/dispatch task-fsm {:type :delete})}])]]])

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
                    :tasks tasks}])]
      [:tfoot
       [task-row
        {:task {:id ""
                :title ""
                :notes ""
                :estimated_time 20
                :estimated_time_map {:hours 0
                                     :minutes 20}
                :due_date nil
                :tracked_time 0
                :parent_task_id ""
                :created_at (new js/Date)
                :updated_at (new js/Date)
                :completed_at nil
                :time_sessions []}}]]]]))

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

(comment
  (let [fsm @debug-atom]
    (fsm/dispatch fsm {:type :update
                       :data {[:title] "Uxternal update"}})))
