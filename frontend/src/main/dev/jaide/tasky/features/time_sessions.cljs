(ns dev.jaide.tasky.features.time-sessions
  (:require
   [promesa.core :as p]
   [dev.jaide.finity.core :as fsm]
   [dev.jaide.valhalla.core :as v]
   [dev.jaide.tasky.dom :refer [interval abort-controller]]
   [dev.jaide.tasky.time-sessions :as time-sessions]
   [dev.jaide.tasky.state.selectors :as select]
   [dev.jaide.tasky.views.table :refer [th td]]))

(def session-fsm-spec
  (fsm/define
    {:id :time-session-fsm

     :initial {:state :idle
               :context {}}

     :states {:idle       {}
              :creating   {:task-id (v/string)
                           :session time-sessions/validator}
              :clocked-in {:session-id (v/string)
                           :elapsed (v/number)
                           :task-id (v/string)}
              :clocking-out {:task-id (v/string)
                             :session-id time-sessions/validator}
              :interrupting {:prev-task-id (v/string)
                             :prev-session-id time-sessions/validator
                             :next-task-id (v/string)
                             :next-session time-sessions/validator}}

     :actions {:set {:task-id (v/string) :session time-sessions/validator}
               :clock-in {:task-id (v/string)}
               :tick {}
               :clock-out {:task-id (v/string)}
               :clocked-out {:task-id (v/string)}
               :created {:session time-sessions/validator}
               :error {:error (v/instance js/Error)}
               :interrupted {:next-session time-sessions/validator
                             :prev-session time-sessions/validator}}

     :effects {:create [{}
                        (fn [{:keys [dispatch state context]}]
                          (let [[signal abort] (abort-controller)]
                            (-> (p/let [session (time-sessions/create (:session context) :signal signal)]
                                  (dispatch {:type :created :session session}))
                                (p/catch
                                 (fn [error]
                                   (dispatch {:type :error :error error}))))
                            abort))]
               :start-clock [{}
                             (fn [{:keys [dispatch]}]
                               (interval 1000 #(dispatch {:type :tick})))]

               :update [{}
                        (fn [{:keys [dispatch state context]}]
                          (let [session (:session context)
                                [signal abort] (abort-controller)]
                            (-> (p/let [session (time-sessions/update session :signal signal)]
                                  (dispatch {:type :clocked-out :task-id (:task_id session)}))
                                (p/catch
                                 (fn [error]
                                   (dispatch {:type :error :error error}))))
                            abort))]

               :interrupt [{}
                           (fn [{:keys [dispatch state context]}]
                             (let [{:keys [next-session prev-session-id prev-task-id]} context
                                   [signal abort] (abort-controller)]
                               (-> (p/let [prev-session (select/session-by-id prev-task-id prev-session-id)
                                           prev-session (time-sessions/update prev-session :signal signal)
                                           next-session (time-sessions/create next-session :signal signal)
                                           prev-session (-> prev-session
                                                            (assoc :interrupted_by_task_id (:id next-session))
                                                            (time-sessions/update :signal signal))]
                                     (dispatch {:type :interrupted
                                                :next-session next-session
                                                :prev-session-id prev-session-id}))
                                   (p/catch
                                    (fn [error]
                                      (dispatch {:type :error :error error}))))
                               abort))]}

     :transitions
     [{:from [:idle]
       :actions [:clock-in]
       :to [:creating]
       :do (fn [_state action]
             {:state :creating
              :context {:task-id (:task-id action)
                        :session {:id ""
                                  :start_time (new js/Date)
                                  :description ""
                                  :task_id (:task-id action)}}
              :effect {:id :create}})}

      {:from [:creating]
       :actions [:created]
       :to [:clocked-in]
       :do (fn [state action]
             {:state :clocked-in
              :context {:task-id (get-in state [:context :task-id])
                        :session-id (get-in action [:session :id])
                        :elapsed (- (js/Date.now) (get-in action [:session :start_time]))}
              :effect {:id :start-clock}})}

      {:from [:clocked-in]
       :actions [:tick]
       :to [:clocked-in]
       :do (fn [state _action]
             (assoc-in state [:context :elapsed] (- (js/Date.now) (get-in state [:context :session :start_time]))))}

      {:from [:clocked-in]
       :actions [:clock-out]
       :to [:clocking-out]
       :do (fn [current action]
             (let [{:keys [context state]} current]
               {:state :clocking-out
                :context {:task-id (:task-id context)
                          :session-id (:session-id context)}
                :effect :update}))}

      {:from [:clocking-out]
       :actions [:clocked-out]
       :to :idle}

      {:from [:clocked-in]
       :actions [:clock-in]
       :to [:interrupting]
       :do (fn [{:keys [_state context]} action]
             {:state :interrupting
              :context {:prev-task-id (:task-id context)
                        :next-task-id (:task-id action)
                        :prev-session-id (:session-id context)
                        :next-session {:id ""
                                       :start_time (new js/Date)
                                       :description ""
                                       :task_id (:task-id action)}}

              :effect {:id :interrupt}})}

      {:from [:interrupting]
       :actions [:interrupted]
       :to [:clocked-in]
       :do (fn [state action]
             (let [session (:next-session action)]
               {:state :clocked-in
                :context {:task-id (:task_id session)
                          :session-id (get-in action [:session :id])
                          :elapsed (- (js/Date.now (get-in action [:session :start_time])))}
                :effect {:id :start-clock}}))}]}))

(defn time-session-row
  [{:keys [time-session]}]
  [:tr
   [td
    {}
    (:started_at time-session)]])

(defn time-sessions-table
  []
  (let [task-fsm @select/selected-task-fsm
        task (:task task-fsm)]
    (println task)
    [:table.min-w-full.table-auto
     [:thead
      {:class ""}
      [:tr
       [th
        {:class "rounded-l-lg"}
        "Started"]
       [th
        {}
        "Ended"]
       [th
        {}
        "Elapsed"]
       [th
        {}
        "Interrupted by"]
       [th
        {:class "rounded-r-lg"}
        ""]]]

     [:tbody
      (doall
       (for [time-session (:time_sessions task)]
         [time-session-row {:key (:id time-session)
                            :time-session time-session}]))]]))

(defn clock-actions
  []
  [:div.flex.flex-row.gap-4
   [:button
    {:class "btn bg-red-500"}
    "Clock Out"]
   [:button
    {:class "btn bg-blue-500"}
    "Clock In"]])
