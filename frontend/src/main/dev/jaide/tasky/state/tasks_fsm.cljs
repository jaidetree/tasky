(ns dev.jaide.tasky.state.tasks-fsm
  (:require
   [clojure.string :as s]
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
                     (when (= (:type action) (:state next) :deleted)
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
                                :fsm (v/instance fsm/AtomFSM)}))}}

     :actions {:fetch {}
               :fetched {:tasks tasks-validator}
               :refresh {}
               :remove {:task-id (v/string)}
               :error {:error (v/instance js/Error.)}}

     :effects {:fetch
               [{}
                (fn [{:keys [dispatch effect]}]
                  (-> (fetch-tasks)
                      (p/then #(dispatch {:type :fetched
                                          :tasks (get % :tasks)}))
                      (p/catch (fn [error]
                                 (js/console.error error)
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
       :actions [:remove]
       :to [:tasks]
       :do (fn remove-task [{:keys [state context]} {:keys [task-id]}]
             (let [tasks (:tasks context)
                   {:keys [fsm unsubscribe]} (->> tasks
                                                  (filter #(= (:id %) task-id))
                                                  (first))]
               (unsubscribe)
               {:state state
                :context {:tasks (->> (:tasks context)
                                      (remove #(= (:id %) task-id))
                                      (vec))
                          :history (:history context)}}))}]}))

(comment
  (->> [1 2 3 4]
       (take-while
        #(not= % 3))))

(def tasks-fsm (ratom-fsm tasks-fsm-spec))

(defn all-tasks
  []
  (->> (get tasks-fsm :tasks)
       (map #(get-in % [:fsm :task]))))

(defn find-task-fsm
  [task-id]
  (println "task-id" task-id)
  (->> (get tasks-fsm :tasks)
       (filter #(= (get % :id) task-id))
       (map :fsm)
       (first)))

#_(fsm/subscribe
   tasks-fsm
   (fn [{:keys [next]}]
     (cljs.pprint/pprint next)))

(comment
  @tasks-fsm)

