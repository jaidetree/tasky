(ns dev.jaide.tasky.state.time-sessions-fsm
  (:require
   [promesa.core :as p]
   [dev.jaide.finity.core :as fsm]
   [dev.jaide.valhalla.core :as v]
   [dev.jaide.tasky.dom :refer [abort-controller]]
   [dev.jaide.tasky.time-sessions :as time-sessions]))

(def sessions-validator (v/hash-map (v/string) time-sessions/validator))

(defn index-sessions
  [sessions]
  (->> sessions
       (reduce
        (fn [m session]
          (assoc m (:id session) session))
        {})))

(def order-validator (v/vector (v/string)))

(def time-sessions-fsm-spec
  (fsm/define
    {:id  :time-sessions-fsm

     :initial {:state :idle
               :context {:order []
                         :sessions {}}}

     :states {:idle {:sessions sessions-validator
                     :order order-validator}
              :creating {:sessions sessions-validator
                         :order order-validator
                         :session time-sessions/validator}
              :updating {:sessions sessions-validator
                         :order order-validator
                         :session time-sessions/validator}}

     :actions {:set {:sessions (v/vector time-sessions/validator)}
               :create {:task-id (v/string)}
               :created {:session time-sessions/validator}
               :update {:session-id (v/string) :data (v/assert map?)}
               :updated {:session time-sessions/validator}
               :error {:error (v/instance js/Error)}}

     :effects {:create [{}
                        (fn [{:keys [dispatch context]}]
                          (let [session (:session context)
                                [signal abort] (abort-controller)]
                            (-> (p/let [session (time-sessions/create session :signal signal)]
                                  (dispatch {:type :created :session session}))
                                (p/catch #(dispatch {:type :error :error %})))
                            abort))]

               :update [{}
                        (fn [{:keys [dispatch context]}]
                          (let [session (:session context)
                                [signal abort] (abort-controller)]
                            (-> (p/let [session (time-sessions/update session :signal signal)]
                                  (dispatch {:type :updated :session session}))
                                (p/catch #(dispatch {:type :error :error %})))
                            abort))]}

     :transitions
     [{:from [:idle]
       :actions [:set]
       :to [:idle]
       :do (fn [_state action]
             (let [{:keys [sessions]} action]
               {:state :idle
                :context {:order (->> sessions
                                      (map :id))
                          :sessions (index-sessions sessions)}}))}

      {:from [:idle]
       :actions [:create]
       :to [:creating]
       :do (fn [{:keys [_state context]} action]
             {:state :creating
              :context {:sessions (:sessions context)
                        :order (:order context)
                        :session {:id ""
                                  :start_time (new js/Date)
                                  :description ""
                                  :task_id (:task-id action)}}
              :effect {:id :create}})}

      {:from [:creating]
       :actions [:created]
       :to [:idle]
       :do (fn [{:keys [_state context]} {:keys [session]}]
             {:state :idle
              :context {:sessions (-> (:sessions context)
                                      (assoc (:id session) session))
                        :order (-> (:order context)
                                   (conj (:id session)))}})}

      {:from [:idle]
       :actions [:update]
       :to [:updating]
       :do (fn [{:keys [_state context] :as current} action]
             (if-let [session (get-in context [:sessions (:session-id action)])]
               (let [session (merge session (:data action))]
                 {:state :updating
                  :context {:sessions (-> (:sessions context)
                                          (assoc (:id session) session))
                            :order (:order context)
                            :session session}})
               (do
                 (js/console.warn (str "Could not update session " (:session-id action)
                                       ": Session not found."))
                 current)))}

      {:from [:updating]
       :actions [:updated]
       :to [:idle]
       :do (fn [{:keys [_state context]} {:keys [session]}]
             {:state :idle
              :context {:sessions (-> (:sessions context)
                                      (assoc (:id session) session))}})}]}))




