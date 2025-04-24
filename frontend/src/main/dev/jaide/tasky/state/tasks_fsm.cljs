(ns dev.jaide.tasky.state.tasks-fsm
  (:require
   [dev.jaide.finity.core :as fsm]
   [dev.jaide.tasky.state-machines :refer [ratom-fsm]]
   [dev.jaide.tasky.state.task-fsm :refer [create-task-fsm]]
   [dev.jaide.tasky.tasks :refer [fetch-tasks tasks-validator]]
   [dev.jaide.valhalla.core :as v]
   [promesa.core :as p]))

(def tasks-fsm-spec
  (fsm/define
    {:id :tasks
     :initial {:state :empty
               :context {:error nil}}

     :states {:empty {:error (v/nilable (v/instance js/Error))}
              :loading {}
              :tasks {:tasks (v/vector
                              (v/instance fsm/AtomFSM))
                      :id (v/nilable (v/string))}}

     :actions {:fetch {}
               :fetched {:tasks tasks-validator}
               :refresh {}
               :error {:error (v/instance js/Error.)}}

     :effects {:fetch
               [{}
                (fn [{:keys [dispatch effect]}]
                  (-> (fetch-tasks)
                      (p/then #(dispatch {:type :fetched
                                          :tasks (get % :tasks)}))
                      (p/catch #(dispatch {:type :error
                                           :error %}))))]}

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
       :do (fn [state action]
             {:state :tasks
              :context {:tasks (->> (:tasks action)
                                    (map create-task-fsm)
                                    (vec))}})}

      {:from [:loading]
       :actions [:error]
       :to [:empty]
       :do (fn [state action]
             {:state :empty
              :context {:error (:error action)}})}

      {:from [:tasks]
       :actions [:refresh]
       :to [:tasks]
       :do (fn [{:keys [state context]} action]
             {:state state
              :context context
              :effect {:id :fetch}})}]}))

(def tasks-fsm (ratom-fsm tasks-fsm-spec))

(fsm/dispatch tasks-fsm {:type :fetch})

#_(fsm/subscribe
   tasks-fsm
   (fn [{:keys [action next]}]
     (cljs.pprint/pprint {:action action
                          :state next})))

(comment
  @tasks-fsm)

