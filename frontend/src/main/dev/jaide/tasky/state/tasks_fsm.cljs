(ns dev.jaide.tasky.state.tasks-fsm
  (:require
   [dev.jaide.finity.core :as fsm]
   [dev.jaide.tasky.state-machines :refer [ratom-fsm]]
   [dev.jaide.tasky.state.task-fsm :refer [create-task-fsm]]
   [dev.jaide.tasky.tasks :refer [fetch-tasks tasks-validator]]
   [dev.jaide.valhalla.core :as v]
   [promesa.core :as p]))

(declare tasks-fsm)

(defn task->fsm
  [task]
  (let [fsm (create-task-fsm task)]
    {:id (:id task)
     :unsubscribe (fsm/subscribe
                   fsm
                   (fn [{:keys [next action]}]
                     (when (and (= (:type action) :delete)
                                (= (:state next) :deleted))
                       (fsm/dispatch tasks-fsm {:type :remove :task-id (:id task)}))))
     :fsm fsm}))

(def tasks-fsm-spec
  (fsm/define
    {:id :tasks
     :initial {:state :empty
               :context {:error nil}}

     :states {:empty {:error (v/nilable (v/instance js/Error))}
              :loading {}
              :tasks {:tasks (v/vector
                              (v/record
                               {:id (v/string)
                                :unsubscribe (v/assert fn?)
                                :fsm (v/instance fsm/AtomFSM)}))
                      :history (v/vector (v/string))}}

     :actions {:fetch {}
               :fetched {:tasks tasks-validator}
               :refresh {}
               :navigate {:task-id (v/string)}
               :back {:target (v/union
                               (v/string)
                               (v/keyword))}
               :remove {:task-id (v/string)}
               :error {:error (v/instance js/Error.)}}

     :effects {:fetch
               [{}
                (fn [{:keys [dispatch effect]}]
                  (-> (fetch-tasks)
                      (p/then #(dispatch {:type :fetched
                                          :tasks (get % :tasks)}))
                      (p/catch (fn [error]
                                 #_(js/console.error error)
                                 (dispatch {:type :error
                                            :error error})))))]}

     :transitions
     [{:from [:empty]
       :actions [:fetch]
       :to [:loading]
       :do (fn [state action]
             {:state :loading
              :context {}
              :effect {:id :fetch}})}

      {:from [:loading :tasks]
       :actions [:fetched]
       :to [:tasks]
       :do (fn loading->tasks [{:keys [_state context]} action]
             {:state :tasks
              :context (merge context
                              {:tasks (->> (get action :tasks [])
                                           (mapv task->fsm))
                               :history (get context :history [])})})}

      {:from [:loading]
       :actions [:error]
       :to [:empty]
       :do (fn loading->empty [state action]
             {:state :empty
              :context {:error (:error action)}})}

      {:from [:tasks]
       :actions [:refresh]
       :to [:tasks]
       :do (fn tasks-refresh [{:keys [state context]} action]
             {:state state
              :context context
              :effect {:id :fetch}})}

      {:from [:tasks]
       :actions [:back]
       :to [:tasks]
       :do (fn tasks-back [{:keys [state context effect]} {:keys [target]}]
             {:state state
              :context (-> context
                           (assoc :history (if (= target :root)
                                             []
                                             (-> (take-while
                                                  #(not= % target)
                                                  (:history context))
                                                 (conj target)
                                                 (vec)))))})}

      {:from [:tasks]
       :actions [:navigate]
       :to [:tasks]
       :do (fn tasks-navigate [{:keys [state context]} action]
             {:state state
              :context (-> context
                           (update :history conj (:task-id action)))})}]}))

(comment
  (->> [1 2 3 4]
       (take-while
        #(not= % 3))))

(def tasks-fsm (ratom-fsm tasks-fsm-spec))

#_(fsm/subscribe
   tasks-fsm
   (fn [{:keys [action next]}]
     (cljs.pprint/pprint {:action action
                          :state next})))

(comment
  @tasks-fsm)

