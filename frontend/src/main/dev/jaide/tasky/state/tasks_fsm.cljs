(ns dev.jaide.tasky.state.tasks-fsm
  (:require
   [promesa.core :as p]
   [dev.jaide.finity.core :as fsm]
   [dev.jaide.valhalla.core :as v]
   [dev.jaide.tasky.tasks :refer [tasks-validator fetch-tasks]]
   [dev.jaide.tasky.state-machines :refer [ratom-fsm]]))

(def tasks-fsm-spec
  (fsm/define
    {:id :router
     :initial {:state :empty
               :context {:error nil}}

     :states {:empty {:error (v/nilable (v/instance js/Error))}
              :loading {}
              :tasks {:tasks tasks-validator}}

     :actions {:fetch {}
               :fetched {:tasks tasks-validator}
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

      {:from [:loading]
       :actions [:fetched]
       :to [:tasks]
       :do (fn [state action]
             {:state :tasks
              :context {:tasks (:tasks action)}})}

      {:from [:loading]
       :actions [:error]
       :to [:empty]
       :do (fn [state action]
             {:state :empty
              :context {:error (:error action)}})}]}))

(def tasks-fsm (ratom-fsm tasks-fsm-spec))

(fsm/dispatch tasks-fsm {:type :fetch})
