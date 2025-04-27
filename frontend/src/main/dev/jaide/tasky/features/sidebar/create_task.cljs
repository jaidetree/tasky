(ns dev.jaide.tasky.features.sidebar.create-task
  (:require
   [reagent.core :as r]
   [clojure.pprint :refer [pprint]]
   [dev.jaide.finity.core :as fsm]
   [dev.jaide.tasky.state-machines :refer [ratom-fsm]]
   [dev.jaide.tasky.state.app-fsm :refer [app-fsm]]
   [dev.jaide.tasky.state.tasks-fsm :refer [tasks-fsm]]
   [dev.jaide.tasky.tasks :as task]
   [dev.jaide.valhalla.core :as v]
   [promesa.core :as p]))

(def error-validator
  (v/instance js/Error))

(def default-task-draft
  {:id ""
   :title ""
   :notes ""
   :estimated_time_map {:hours 0
                        :minutes 20}
   :due_date ""
   :parent_task_id ""})

(def new-task-fsm-spec
  (fsm/define
    {:id :new-task

     :initial {:state :inactive
               :context {}}

     :states  {:inactive {}
               :drafting {:task task/draft-validator
                          :dirty (v/boolean)
                          :error (v/nilable error-validator)}
               :saving {:task task/draft-validator}}

     :actions {:create {:defaults (v/assert map?)}
               :update {:data (v/hash-map
                               (v/vector (v/keyword))
                               (v/string))}
               :save  {}
               :cancel {}
               :saved {:task task/task-validator}
               :error {:error error-validator}}

     :effects {:save [{:task task/draft-validator}
                      (fn [{:keys [dispatch effect]}]
                        (let [task (:task effect)
                              {:keys [hours minutes]} (get task :estimated_time_map)
                              task (-> task
                                       (assoc :estimated_time (+ minutes (* hours 60))))]
                          (-> (task/create-task task)
                              (p/then #(dispatch {:type :saved
                                                  :task %}))
                              (p/catch #(dispatch {:type :error
                                                   :error %})))))]}

     :transitions
     [{:from [:inactive]
       :actions [:create]
       :to [:drafting]
       :do (fn [state action]
             {:state :drafting
              :context {:task default-task-draft
                        :dirty false
                        :error nil}})}

      {:from [:drafting]
       :actions [:update]
       :to [:drafting]
       :do (fn [{:keys [state context]} action]
             (let [draft (->> (:data action)
                              (reduce
                               (fn [task [path v]]
                                 (assoc-in task path v))
                               (:task context)))]
               {:state state
                :context {:task draft
                          :dirty (not= default-task-draft draft)
                          :error (:error context)}}))}

      {:from [:drafting]
       :actions [:cancel]
       :to :inactive}

      {:from [:saving]
       :actions [:cancel]
       :to [:drafting]
       :do (fn [{:keys [state context]} action]
             {:state :drafting
              :context {:task (:task context)
                        :dirty true
                        :error nil}})}

      {:from [:saving]
       :actions [:saved]
       :to [:drafting]
       :do (fn [state action]
             (let [task (get-in state [:context :task])]
               {:state :drafting
                :context {:task (merge task
                                       (select-keys default-task-draft
                                                    [:title
                                                     :notes
                                                     :estimated_time_map]))
                          :dirty false
                          :error nil}}))}

      {:from [:saving]
       :actions [:error]
       :to [:drafting]
       :do (fn [{:keys [context]} action]
             (let [{:keys [task]} context
                   {:keys [error]} action]
               {:state :drafting
                :context {:task task
                          :dirty true
                          :error error}}))}

      {:from [:drafting]
       :actions [:save]
       :to [:saving :drafting]
       :do (fn [state _action]
             (if (get-in state [:context :dirty])
               (let [task (get-in state [:context :task])
                     {:keys [hours minutes]} (get task :estimated_time_map)]
                 {:state :saving
                  :context {:task task}
                  :effect {:id :save
                           :task (-> task
                                     (assoc :estimated_time (+ minutes (* hours 60))))}})
               state))}]}))

(def new-task-fsm
  (ratom-fsm new-task-fsm-spec))

(fsm/subscribe
 new-task-fsm
 (fn [{:keys [next action]}]
   (cljs.pprint/pprint action)
   (cljs.pprint/pprint next)
   (when (= (:type action) :saved)
     (fsm/dispatch tasks-fsm {:type :refresh}))))

(pprint @new-task-fsm)

(defn create-task
  [event]
  (.preventDefault event)
  (fsm/dispatch new-task-fsm {:type :save}))

(defn update-task
  [event]
  (let [name (keyword (.. event -target -name))
        value (.. event -target -value)
        data (case name
               :estimated_time_hours   {[:estimated_time_map :hours] value}
               :estimated_time_minutes {[:estimated_time_map :minutes] value}
               {[name] value})
        action {:type :update :data data}]
    (fsm/dispatch new-task-fsm action)))

(defn field
  [{:keys [label id]} & children]
  (into
   [:section
    [:label.block.py-2
     {:htmlFor id}
     label]]
   children))

(defn title-field
  []
  (let [title (get-in new-task-fsm [:task :title])]
    [field
     {:id "id_title"
      :label "Title"}
     [:input {:id "id_title"
              :type :text
              :name :title
              :value title
              :class "bg-stone-700/20 p-2 rounded-sm w-full"}]]))

(defn notes-field
  []
  (let [notes (get-in new-task-fsm [:task :notes])]
    [field
     {:id "id_notes"
      :label "Notes"}
     [:textarea
      {:name "notes"
       :id "id_notes"
       :class "bg-stone-700/20 p-2 rounded-sm w-full h-40"
       :value notes}]]))

(defn estimated-time-field
  []
  (let [{:keys [hours minutes]} (get-in new-task-fsm [:task :estimated_time_map])]
    [field
     {:id "id_estimated_time"
      :label "Estimate"}
     [:div.flex.flex-row.gap-2.items-center.justify-evenly
      [:div.flex.flex-col.gap-2.items-center
       [:input
        {:type :range
         :name "estimated_time_hours"
         :min "0"
         :max "23"
         :step "1"
         :class "block bg-stone-700/20 p-2 rounded-sm flex-shrink w-full"
         :value hours}]
       [:span
        [:output
         (str hours " hrs")]]]
      [:div.flex.flex-col.gap-2.items-center
       [:input
        {:type :range
         :name "estimated_time_minutes"
         :min "0"
         :max "59"
         :class "block bg-stone-700/70 p-2 rounded-sm flex-shrink w-full"
         :value minutes}]
       [:span
        [:output
         (str minutes " mins")]]]]]))

(defn due-date-field
  []
  (let [due-date (get-in new-task-fsm [:task :due_date])]
    [field
     {:id "id_due_date"
      :label "Due Date"}
     [:input
      {:id "id_due_date"
       :type "datetime-local"
       :value due-date
       :class "bg-stone-700/20 p-2 rounded-sm w-full"}]]))

(defn parent-task-field
  []
  (let [parent-task-id (get-in new-task-fsm [:task :parent_task_id])
        tasks (get tasks-fsm :tasks [])]
    [field
     {:id "id_parent_task_id"
      :label "Parent Task"}
     [:select
      {:id "id_parent_task_id"
       :class "bg-stone-700/20 p-2 rounded-sm w-full"
       :name "parent_task_id"
       :value parent-task-id}
      [:option {:value ""} "-- No Parent Task --"]
      (for [task tasks]
        [:option
         {:value (:id task) :key (:id task)}
         (:title task)])]]))

(defn new-task-form
  []
  (r/with-let [_ (fsm/dispatch new-task-fsm {:type :create :defaults {}})]
    [:form.gap-2
     {:on-submit create-task
      :on-input update-task}
     [:header
      [:h2.text-xl "New Task"]]
     [:div.flex.flex-col.gap-2
      [title-field]
      [notes-field]
      [estimated-time-field]
      [due-date-field]
      [parent-task-field]
      [:section.flex.flex-row.justify-end.items-center.gap-2.py-4
       [:button
        {:type "submit"
         :class "btn py-2 px-4 text-white bg-blue-500"}
        "Create"]]]]
    (finally
      (fsm/dispatch new-task-fsm {:type :cancel}))))

(comment
  (js/alert "hi")
  @new-task-fsm
  (fsm/dispatch new-task-fsm {:type :cancel}))
