(ns dev.jaide.tasky.state.task-fsm
  (:require
   [promesa.core :as p]
   [dev.jaide.valhalla.core :as v]
   [dev.jaide.finity.core :as fsm]
   [dev.jaide.tasky.dom :refer [interval abort-controller]]
   [dev.jaide.tasky.state-machines :refer [ratom-fsm]]
   [dev.jaide.tasky.tasks :as tasks]
   [dev.jaide.tasky.time-sessions :as time-sessions]))

(def error-validator
  (v/instance js/Error))

(def blank-task
  {:id ""
   :title ""
   :description ""
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

(defn- index-sessions
  [sessions]
  (->> sessions
       (reduce
        (fn [idx-map session]
          (assoc idx-map (:id session) session))
        {})))

(defn task->fsm-context
  [task]
  (let [sessions (:time_sessions task)]
    {:task task
     :sessions {:all (index-sessions sessions)
                :order (->> sessions
                            (map :id)
                            (vec))}}))

(defn session->start-timestamp
  [{:keys [start_time]}]
  (.getTime start_time))

(defn calc-elapsed
  [start-timestamp]
  (-> (- (js/Date.now) start-timestamp)
      (/ 1000)
      (js/Math.round)))

(def task-fsm-spec
  (fsm/define
    {:id :task

     :initial {:state           :ready
               :context         {:task blank-task
                                 :sessions {:all {}
                                            :order []}}}

     :states {:ready            {:task tasks/task-validator
                                 :sessions time-sessions/time-sessions
                                 :error (v/nilable error-validator)}
              :clocking-in      {:task tasks/task-validator
                                 :sessions time-sessions/time-sessions}
              :clocked-in       {:task tasks/task-validator
                                 :elapsed (v/number)
                                 :session-id (v/string)
                                 :sessions time-sessions/time-sessions
                                 :error (v/nilable error-validator)}
              :clocking-out     {:task tasks/task-validator
                                 :sessions time-sessions/time-sessions}
              :submitting       {:task tasks/task-validator
                                 :sessions time-sessions/time-sessions}
              :deleting         {:task tasks/task-validator
                                 :sessions time-sessions/time-sessions}
              :deleted          {}}

     :actions {:init            {:task tasks/task-validator}
               :update          {:data (v/hash-map
                                        (v/vector (v/keyword))
                                        (v/union
                                         (v/nil-value)
                                         (v/string)
                                         (v/number)
                                         (v/boolean)
                                         (v/instance js/Date)))}
               :save            {}
               :updated         {:task tasks/task-validator}
               :clock-in        {}
               :clock-out       {:data (v/nilable (v/assert map?))}
               :clocked-in      {:session time-sessions/time-session}
               :sync-clock      {:session time-sessions/time-session}
               :tick            {:elapsed (v/number)}
               :update-session  {:session-id (v/string)
                                 :data (v/assert map?)}
               :session-updated {:session time-sessions/time-session}
               :error           {:error error-validator}
               :delete          {}
               :deleted         {}}

     :effects {#_#_:debounce [{:delay (v/number)
                               :timestamp (v/number)
                               :action (v/keyword)}
                              (fn [{:keys [dispatch effect]}]
                                (let [ms (get effect :delay)
                                      action (get effect :action)]
                                  (timeout
                                   ms
                                   #(dispatch {:type action}))))]

               :update {:args {:timestamp (v/number)}
                        :do (fn [{:keys [dispatch] {:keys [task]} :context} effect]
                              (let [[signal abort] (abort-controller)]
                                (-> (tasks/update-task task signal)
                                    (p/then #(dispatch {:type :updated :task %}))
                                    (p/catch #(dispatch {:type :error :error %})))
                                (fn []
                                  (abort))))}

               :delete {:args {:task-id (v/string)}
                        :do (fn [{:keys [fsm dispatch]} effect]
                              (-> (tasks/delete-task (:task-id effect)) #_(p/timeout 100 nil)
                                  (p/then #(fsm/destroy fsm))
                                  (p/catch #(dispatch {:type :error :error %}))))}

               :clock-in {:args {:session time-sessions/time-session}
                          :do (fn [{:keys [dispatch context]} {:keys [session]}]
                                (let [[signal abort] (abort-controller)]
                                  (-> (p/let [session (time-sessions/create session :signal signal)]
                                        (dispatch {:type :clocked-in :session session}))
                                      (p/catch #(dispatch {:type :error :error %})))
                                  abort))}

               :update-session {:args {:session time-sessions/time-session}
                                :do (fn [{:keys [dispatch context]} {:keys [session]}]
                                      (let [[signal abort] (abort-controller)]
                                        (-> (p/let [session (time-sessions/update session :signal signal)]
                                              (dispatch {:type :session-updated :session session}))
                                            (p/catch #(dispatch {:type :error :error %})))
                                        abort))}

               :task-timer {:args {:start-time (v/number)}
                            :do (fn [{:keys [dispatch]} {:keys [start-time]}]
                                  (interval 1000
                                            #(dispatch {:type :tick :elapsed (calc-elapsed start-time)})))}}

     :transitions
     [{:from [:ready]
       :actions [:update]
       :to [:submitting]
       :do (fn [{:keys [context]} {:keys [data]}]
             (let [task (:task context)
                   task (->> data
                             (reduce
                              (fn [task [path value]]
                                (assoc-in task path value))
                              task))]
               {:state :submitting
                :context {:error (:error context)
                          :sessions (:sessions context)
                          :task task}
                :effects {:update {:timestamp (js/Date.now)}}}))}

      {:from [:submitting]
       :actions [:updated]
       :to [:ready]
       :do (fn [_state action]
             {:state :ready
              :context (assoc
                        (task->fsm-context (:task action))
                        :error nil)})}

      {:from [:submitting :deleting]
       :actions [:error]
       :to [:ready]
       :do (fn [state action]
             {:state :ready
              :context {:task (get-in state [:context :task])
                        :sessions (get-in state [:context :sessions])
                        :error (get :error action)}})}

      {:from [:ready]
       :actions [:delete]
       :to [:deleting]
       :do (fn [state _action]
             {:state :deleting
              :context {:task (get-in state [:context :task])
                        :sessions (get-in state [:context :sessions])}})}

      {:from [:deleting]
       :actions [:deleted]
       :to [:deleted]
       :do (fn [state _action]
             {:state :deleted
              :effects {:delete {:task-id (get-in state [:context :task :id])}}})}

      {:from [:ready]
       :actions [:sync-clock]
       :to [:clocked-in]
       :do (fn [{:keys [context effects]} {:keys [session]}]
             (let [start-time (session->start-timestamp session)]
               {:state :clocked-in
                :context (-> context
                             (merge {:elapsed (calc-elapsed start-time)
                                     :session-id (:id session)}))
                :effects {:task-timer {:start-time start-time}}}))}

      {:from [:ready]
       :actions [:clock-in]
       :to [:clocking-in]
       :do (fn [{:keys [context]} _action]
             {:state :clocking-in
              :context context
              :effects {:clock-in {:session {:id ""
                                             :description ""
                                             :start_time (new js/Date)
                                             :task_id (get-in context [:task :id])}}}})}

      {:from [:clocking-in]
       :actions [:clocked-in]
       :to [:clocked-in]
       :do (fn [{:keys [context]} {:keys [session]}]
             (let [start-time (-> session
                                  (session->start-timestamp))]

               {:state :clocked-in
                :context (-> context
                             (assoc :elapsed (calc-elapsed start-time))
                             (assoc :session-id (:id session))
                             (assoc-in [:sessions :all (:id session)] session)
                             (update-in [:sessions :order] conj (:id session)))
                :effects {:task-timer {:start-time start-time}}}))}

      {:from [:clocked-in]
       :actions [:tick]
       :to [:clocked-in]
       :do (fn [{:keys [context effects]} {:keys [elapsed]}]
             {:state :clocked-in
              :context (assoc context :elapsed elapsed)
              :effects effects})}

      {:from [:clocked-in]
       :actions [:update]
       :to [:clocked-in :clocking-out]
       :do (fn [{:keys [context effects]} {:keys [data]}]
             (let [task (:task context)
                   task (->> data
                             (reduce
                              (fn [task [path value]]
                                (assoc-in task path value))
                              task))]
               (if (instance? js/Date (:completed_at task))
                 (let [session-id (:session-id context)
                       session (-> context
                                   (get-in [:time-sessions :all session-id])
                                   (merge
                                    {:end_time (new js/Date)}
                                    data))]
                   {:state :clocking-out
                    :context {:error (:error context)
                              :sessions (:sessions context)
                              :task task}
                    :effects {:update {:timestamp (js/Date.now)}
                              :update-session {:dispatch :clocked-out
                                               :session session}}})
                 {:state :clocked-in
                  :context (merge
                            context
                            {:task task})
                  :effects (merge effects
                                  {:update {:timestamp (js/Date.now)}})})))}

      {:from [:clocked-in]
       :actions [:update-session]
       :to [:clocked-in]
       :do (fn [{:keys [context effects]} {:keys [session-id data]}]
             (let [session (-> (get-in context [:time-sessions :all session-id])
                               (merge data))]
               {:state :clocked-in
                :context (-> context
                             (assoc-in [:time-sessions :all session-id] session))
                :effects (assoc effects :update-session {:session session})}))}

      {:from [:clocked-in]
       :actions [:clock-out]
       :to [:clocking-out]
       :do (fn [{:keys [context]} {:keys [data]}]
             (let [session-id (:session-id context)
                   session (-> context
                               (get-in [:sessions :all session-id])
                               (merge
                                {:end_time (new js/Date)}
                                data))]
               {:state :clocking-out
                :context (select-keys context [:task :sessions])
                :effects {:update-session {:dispatch :clocked-out
                                           :session session}}}))}

      {:from [:clocking-out :ready]
       :actions [:session-updated]
       :to [:ready]
       :do (fn [{:keys [context]} {:keys [session]}]
             {:state :ready
              :context (-> context
                           (select-keys [:task :sessions])
                           (assoc-in [:sessions :all (:id session)] session))})}

      {:from [:ready]
       :actions [:update-session]
       :to [:ready]
       :do (fn [{:keys [context]} {:keys [session-id data]}]
             (let [session (-> (get-in context [:time-sessions :all session-id])
                               (merge data))]
               {:state :ready
                :context (-> context
                             (assoc-in [:time-sessions :all (:id session)] session))
                :effects {:update-session {:session session}}}))}]}))

(defn create-task-fsm
  [task]
  (ratom-fsm task-fsm-spec
             {:id (str "task-fsm-" (:id task))
              :initial {:state :ready
                        :context (task->fsm-context task)}}))
(def new-task-fsm-spec
  (fsm/define
    {:id :new-task

     :initial {:state :empty
               :context {:task blank-task}}

     :states {:empty {:task (v/nilable tasks/task-validator)}
              :drafting {:task tasks/draft-validator
                         :error (v/nilable error-validator)}
              :submitting {:task tasks/task-validator}}

     :actions {:update {:data (v/hash-map
                               (v/vector (v/keyword))
                               (v/union
                                (v/string)
                                (v/number)
                                (v/boolean)))}
               :submit {:form-data tasks/draft-validator}
               :created {:task tasks/task-validator}
               :error {:error error-validator}
               :reset {}}

     :effects {:create {:args {:timestamp (v/number)}
                        :do (fn [{:keys [dispatch] {:keys [task]} :context} _effect]
                              (let [[signal abort] (abort-controller)]
                                (-> (tasks/create-task task signal)
                                    (p/then #(dispatch {:type :created :task %}))
                                    (p/catch #(dispatch {:type :error :error %})))
                                (fn []
                                  (abort))))}}

     :transitions
     [{:from [:drafting :empty]
       :actions [:update]
       :to [:drafting]
       :do (fn [{:keys [context]} {:keys [data]}]
             (let [task (:task context)
                   task (->> data
                             (reduce
                              (fn [task [path value]]
                                (assoc-in task path value))
                              task))]
               {:state :drafting
                :context {:error (:error context)
                          :task task}}))}

      {:from [:drafting]
       :actions [:submit]
       :to [:submitting]
       :do (fn [state {:keys [form-data]}]
             (let [task (get-in state [:context :task])]
               {:state :submitting
                :context {:task (merge
                                 blank-task
                                 task
                                 form-data)}
                :effects {:create {:timestamp (js/Date.now)}}}))}

      {:from [:submitting]
       :actions [:error]
       :to [:drafting]
       :do (fn [{:keys [context]} {:keys [error]}]
             {:state :drafting
              :context {:error error
                        :task (:task context)}})}

      {:from [:submitting]
       :actions [:reset :created]
       :to [:empty]
       :do (fn [_state action]
             {:state :empty
              :context {:task (-> blank-task
                                  (assoc :parent_task_id (get-in action [:task :parent_task_id])))}})}]}))

(comment
  nil
  nil)
