(ns dev.jaide.tasky.state.time-sessions-fsm
  (:require
   [promesa.core :as p]
   [dev.jaide.finity.core :as fsm]
   [dev.jaide.valhalla.core :as v]
   [dev.jaide.tasky.time-sessions :as time-sessions]
   [dev.jaide.tasky.state-machines :refer [ratom-fsm]]
   [dev.jaide.tasky.state.tasks-fsm :refer [tasks-fsm]]
   [dev.jaide.tasky.state.selectors :as select]
   [dev.jaide.tasky.utils :as u]))

(defn find-task-fsm
  [task-id]
  (let [task-fsms @select/all-task-fsms]
    (u/find #(= (get-in % [:task :id]) task-id) task-fsms)))

(def time-sessions-fsm-spec
  (fsm/define
    {:id  :time-sessions-fsm

     :initial {:state :idle
               :context {}}

     :states {:idle {}
              :clocked-in {:task-id (v/string)
                           :task-fsm (v/instance fsm/AtomFSM)}}

     :actions {:clock-in {:task-id (v/string)}
               :clock-out {}
               :init      {:task-fsm (v/instance fsm/AtomFSM)
                           :session time-sessions/time-session}
               :reset {}}

     :effects {:clock-in (fn [{:keys [context]}]
                           (let [{:keys [task-fsm]} context]
                             (fsm/dispatch task-fsm {:type :clock-in})))

               :clock-out {:args {:task-fsm (v/instance fsm/AtomFSM)
                                  :interrupt-id (v/nilable (v/string))}
                           :do (fn [_opts {:keys [task-fsm interrupt-id]}]
                                 (fsm/dispatch task-fsm {:type :clock-out
                                                         :data {:interrupted_by_task_id interrupt-id}}))}

               :sync-clock-in {:args {:session time-sessions/time-session}
                               :do (fn [{:keys [dispatch context]} {:keys [session]}]
                                     (let [{:keys [task-fsm]} context]
                                       (fsm/dispatch task-fsm {:type :sync-clock
                                                               :session session})))}

               :sync-clock-out (fn [{:keys [dispatch context]}]
                                 (let [{:keys [task-fsm]} context]
                                   (fsm/subscribe task-fsm
                                                  (fn [{:keys [next action]}]
                                                    (when (and (= (:state next) :ready)
                                                               (= (:type action) :clocked-out))
                                                      (dispatch {:type :reset}))))))}

     :transitions
     [{:from [:idle]
       :actions [:init]
       :to [:clocked-in]
       :do (fn [_current {:keys [task-fsm session]}]
             (let [task-id (get-in task-fsm [:task :id])]
               {:state :clocked-in
                :context {:task-id task-id
                          :task-fsm task-fsm}
                :effects {:sync-clock-in {:session session}
                          :sync-clock-out {}}}))}

      {:from [:idle]
       :actions [:clock-in]
       :to [:clocked-in]
       :do (fn [_current {:keys [task-id]}]
             (let [task-fsm (find-task-fsm task-id)]
               (assert task-fsm (str "Could not find task " task-id))
               {:state :clocked-in
                :context {:task-id task-id
                          :task-fsm task-fsm}
                :effects {:clock-in {}
                          :sync-clock-out {}}}))}

      {:from [:clocked-in]
       :actions [:clock-in]
       :to [:clocked-in]
       :do (fn [{:keys [context]} {:keys [task-id]}]
             (let [prev-fsm (:task-fsm context)
                   task-fsm (find-task-fsm task-id)]
               {:state :clocked-in
                :context {:task-id task-id
                          :task-fsm task-fsm}
                :effects {:clock-out {:task-fsm prev-fsm
                                      :interrupt-id task-id}
                          :clock-in {}
                          :sync-clock-out {}}}))}

      {:from [:clocked-in]
       :actions [:clock-out]
       :to [:idle]
       :do (fn [{:keys [context]} {:keys [task-id]}]
             (let [{:keys [task-fsm]} context]
               {:state :idle
                :context {}
                :effects {:clock-out {:task-fsm task-fsm}}}))}

      {:from [:clocked-in]
       :actions [:reset]
       :to :idle}]}))

(def time-sessions-fsm (ratom-fsm time-sessions-fsm-spec))

(defn clock-in
  [task-id]
  (fsm/dispatch time-sessions-fsm {:type :clock-in :task-id task-id}))

(defn clock-out
  []
  (fsm/dispatch time-sessions-fsm {:type :clock-out}))

(defn- active-time-session
  [task-fsm]
  (let [sessions (-> (get-in task-fsm [:sessions :all])
                     (vals))]
    (when-let [session (u/find #(nil? (:end_time %)) sessions)]
      [task-fsm session])))

(fsm/subscribe
 tasks-fsm
 (fn [{:keys [next]}]
   (let [task-fsms (or @select/all-task-fsms [])]
     (when-let [[task-fsm session] (some active-time-session task-fsms)]
       (fsm/dispatch time-sessions-fsm {:type :init
                                        :task-fsm task-fsm
                                        :session session})
       #_(cljs.pprint/pprint @time-sessions-fsm)))))

(comment
  (cljs.pprint/pprint @time-sessions-fsm))

