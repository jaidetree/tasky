(ns dev.jaide.tasky.state.task-fsm
  (:require
   [promesa.core :as p]
   [dev.jaide.finity.core :as fsm]
   [dev.jaide.tasky.dom :refer [timeout]]
   [dev.jaide.tasky.state-machines :refer [ratom-fsm]]
   [dev.jaide.tasky.tasks :as tasks]
   [dev.jaide.valhalla.core :as v]))

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
    {:id :task

     :initial {:state :ready
               :context {:task blank-task}}

     :states {:ready {:task tasks/task-validator
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
                                (v/boolean)
                                (v/instance js/Date)))}
               :save {}
               :updated {:task tasks/task-validator}
               :error {:error error-validator}
               :delete {}
               :deleted {}}

     :effects {#_#_:debounce [{:delay (v/number)
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
                          (let [abort-controller (js/AbortController.)
                                task (-> task
                                         (update :estimated_time #(if (zero? %)
                                                                    nil
                                                                    %)))]
                            (-> (tasks/update-task task (.-signal abort-controller))
                                (p/then #(dispatch {:type :updated :task %}))
                                (p/catch #(dispatch {:type :error :error %})))
                            (fn []
                              (.abort abort-controller))))]

               :delete [{:task tasks/task-validator}
                        (fn [{:keys [dispatch effect]}]
                          (let [task (:task effect)]
                            (-> (tasks/delete-task (:id task))
                                (p/catch #(dispatch {:type :error :error %})))
                            (timeout
                             500
                             #(dispatch {:type :deleted}))))]}

     :transitions
     [{:from [:ready]
       :actions [:update]
       :to [:saving]
       :do (fn [{:keys [context]} {:keys [data]}]
             (let [task (:task context)]
               {:state :saving
                :context {:error (:error context)
                          :task (->> data
                                     (reduce
                                      (fn [task [path value]]
                                        (assoc-in task path value))
                                      task))}
                :effect {:id :update :timestamp (js/Date.now)}}))}

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

(defn create-task-fsm
  [task]
  (ratom-fsm task-fsm-spec
             {:id (str "task-fsm-" (:id task))
              :initial {:state :ready
                        :context {:task task}}}))

(def new-task-fsm-spec
  (fsm/define
    {:id :new-task

     :initial {:state :empty
               :context {:task blank-task}}

     :states {:empty {:task (v/nilable tasks/task-validator)}
              :drafting {:task tasks/task-validator
                         :error (v/nilable error-validator)}
              :saving {:task tasks/task-validator}}

     :actions {:update {:data (v/hash-map
                               (v/vector (v/keyword))
                               (v/union
                                (v/string)
                                (v/number)
                                (v/boolean)))}
               :save {}
               :created {:task tasks/task-validator}
               :error {:error error-validator}
               :reset {}}

     :effects {:create [{:timestamp (v/number)}
                        (fn [{{{:keys [task]} :context} :state :keys [dispatch _effect]}]
                          (let [abort-controller (js/AbortController.)]
                            (-> (tasks/create-task task (.-signal abort-controller))
                                (p/then #(dispatch {:type :created :task %}))
                                (p/catch #(dispatch {:type :error :error %})))
                            (fn []
                              (.abort abort-controller))))]}

     :transitions
     [{:from [:drafting :empty]
       :actions [:update]
       :to [:drafting]
       :do (fn [{:keys [context]} {:keys [data]}]
             (let [task (:task context)]
               {:state :drafting
                :context {:error (:error context)
                          :task (->> data
                                     (reduce
                                      (fn [task [path value]]
                                        (assoc-in task path value))
                                      task))}}))}

      {:from [:drafting]
       :actions [:save]
       :to [:saving]
       :do (fn [state _action]
             (let [task (get-in state [:context :task])]
               {:state :saving
                :context {:task task}
                :effect {:id :create
                         :timestamp (js/Date.now)}}))}

      {:from [:saving]
       :actions [:error]
       :to [:drafting]
       :do (fn [{:keys [context]} {:keys [error]}]
             {:state :drafting
              :context {:error error
                        :task (:task context)}})}

      {:from [:saving]
       :actions [:reset :created]
       :to [:empty]
       :do (fn [_state action]
             {:state :empty
              :context {:task (-> blank-task
                                  (assoc :parent_task_id (get-in action [:task :parent_task_id])))}})}]}))
