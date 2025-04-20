(ns dev.jaide.tasky.state.task-fsm
  (:require
   [promesa.core :as p]
   [dev.jaide.finity.core :as fsm]
   [dev.jaide.valhalla.core :as v]
   [dev.jaide.tasky.tasks :refer [task-validator session-validator fetch-task]]
   [dev.jaide.tasky.state-machines :refer [ratom-fsm]]))

(def task-fsm-spec
  (fsm/define
    {:id :task
     :initial {:state :inactive
               :context {}}

     :states {:inactive {}
              :fetching {:task-id (v/string)}
              :active {:task task-validator}
              :in-progress {:task task-validator
                            :session session-validator}
              :error {:error (v/instance js/Error)
                      :task-id (v/string)}}

     :actions {:open {:task-id (v/string)}
               :fetched {:task task-validator}
               :clock-in
               :clock-out
               :update-task {:data (v/assert map?)}
               :error {:error (v/instance js/Error) :task-id (v/string)}}

     :effects {:fetch [{:task-id (v/string)}
                       (fn [{:keys [dispatch effect]}]
                         (let [id (:task-id effect)]
                           (-> (fetch-task {:task-id id})
                               (p/then #(dispatch {:type :fetched :task %}))
                               (p/catch #(do
                                           (js/console.error %)
                                           (dispatch {:type :error :error % :task-id id}))))))]}

     :transitions
     [{:from [:inactive]
       :actions [:open]
       :to [:fetching]
       :do (fn [_state action]
             {:state :fetching
              :context {:task-id (:task-id action)}
              :effect {:id :fetch :task-id (:task-id action)}})}

      {:from [:fetching]
       :actions [:fetched]
       :to [:active]
       :do (fn [_state action]
             {:state :active
              :context {:task (:task action)}})}

      {:from [:fetching]
       :actions [:error]
       :to [:error]
       :do (fn [_state action]
             {:state :error
              :context {:error (:error action)
                        :task-id (:task-id action)}})}

      {:from [:active]
       :actions [:clock-in]
       :to [:in-progress]
       :do (fn [state action]
             {:state :in-progress
              :context {:task (get-in state [:context :task])
                        :session {:id ""
                                  :start_time (js/Date.)
                                  :end_time nil
                                  :interrupted_by_task_id nil
                                  :notes ""}}})}

      {:from [:active]
       :actions [:clock-out]
       :to [:active]
       :do (fn [state _action]
             {:state :active
              :context (let [session (assoc (get-in state [:context :session])
                                            :end_time (js/Date.))]
                         {:task (-> (get-in state [:context :task])
                                    (update :sessions conj session))})})}

      {:from [:active :in-progress]
       :actions [:update-task]
       :to [:active :in-progress]
       :do (fn [{:keys [state context]} action]
             {:state state
              :context (update context
                               :task merge (:data action))})}]}))

(def task-fsm (ratom-fsm task-fsm-spec))
