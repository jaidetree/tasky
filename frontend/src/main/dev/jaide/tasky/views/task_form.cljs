(ns dev.jaide.tasky.views.task-form
  (:require
   [clojure.string :as s]
   [reagent.core :refer [with-let]]
   [dev.jaide.finity.core :as fsm]
   [dev.jaide.valhalla.core :as v]
   [dev.jaide.valhalla.js :as vjs]
   [dev.jaide.tasky.router :as router]
   [dev.jaide.tasky.state-machines :refer [ratom-fsm]]
   [dev.jaide.tasky.state.selectors :as select]
   [dev.jaide.tasky.state.task-fsm :refer [new-task-fsm-spec]]
   [dev.jaide.tasky.state.tasks-fsm :refer [tasks-fsm]]))

(defn title
  [{:keys [value]}]
  [:input.flex-grow
   {:name "title"
    :class "text-sm w-full bg-slate-600/30 p-2 rounded"
    :value value
    :placeholder "New Task Title"}])

(defn completed?
  [task]
  (some? (:completed_at task)))

(defn checkbox
  [{:keys [checked]}]
  [:input
   {:type "checkbox"
    :name "complete"
    :checked checked}])

(def estimates
  [["10 min" 10]
   ["15 min" 15]
   ["20 min" 20]
   ["30 min" 30]
   ["45 min" 45]
   ["1 hr" 60]
   ["2 hr" (* 2 60)]
   ["3 hr" (* 3 60)]
   ["4 hr" (* 4 60)]
   ["6 hr" (* 6 60)]
   ["8 hr" (* 8 60)]
   ["1 day" (* 24 60)]
   ["2 days" (* 2 24 60)]
   ["3 days" (* 3 24 60)]
   ["1 week" (* 7 24 60)]
   ["2 weeks" (* 2 7 24 60)]
   ["3 weeks" (* 3 7 24 60)]
   ["1 month" (* 30 24 60)]
   ["2 months" (* 2 30 24 60)]
   ["1 quarter" (* 3 30 24 60)]])

(defn- min->str
  [minutes]
  (let [minutes (js/Number minutes)]
    (loop [estimates estimates]
      (let [[[label estimate] & estimates] estimates]
        (cond
          (nil? estimate)
          "unknown"

          (= estimate minutes)
          label

          :else
          (recur estimates))))))

(comment
  (min->str (* 7 24 60)))

(defn estimate
  [{:keys [form-id value id]}]
  [:select
   {:form form-id
    :class "text-sm border border-slate-700 b-2 rounded p-2 w-full"
    :name "estimated_time"
    :value (js/String value)
    :id id}
   [:option
    {:value "0"}
    "-- unknown --"]
   (for [[label min] estimates]
     [:option
      {:key min
       :value min}
      label])])

(defn date->string
  [date]
  (if (instance? js/Date date)
    (let [year (.getFullYear date)
          month (-> date .getMonth inc .toString (.padStart 2 "0"))
          day (-> date .getDate .toString (.padStart 2 "0"))
          hour (-> date .getHours .toString (.padStart 2 "0"))
          min (-> date .getMinutes .toString (.padStart 2 "0"))]
      (str year "-" month "-" day "T" hour ":" min))
    ""))

(defn due-date
  [{:keys [form-id value]}]
  [:input
   {:form form-id
    :class "text-xs w-full border border-slate-700 p-2 rounded w-full"
    :type "datetime-local"
    :name "due_date"
    :min (date->string (new js/Date))
    :value value}])

(defn task-parents
  [tasks-by-id parent-id & {:keys [level] :or {level 0}}]
  (loop [parent-id parent-id
         tasks []]
    (if (nil? parent-id)
      tasks
      (let [task (get tasks-by-id parent-id)]
        (recur
         (:parent_task_id task)
         (conj tasks (:title task)))))))

(defn tasks->options
  [tasks tasks-by-id]
  (for [task tasks]
    {:label
     (->> (concat
           [(:title task)]
           (task-parents tasks-by-id (:parent_task_id task)))
          (s/join " < "))
     :value (:id task)}))

(defn parent-task
  [{:keys [form-id value options]}]
  [:select
   {:name "parent_task_id"
    :class "text-sm border border-slate-700 b-2 rounded p-2 w-full"
    :value value
    :form form-id}
   [:option
    {:value ""}
    "-- No Parent Task --"]
   (for [option options]
     [:option
      {:key (:value option)
       :value (:value option)}
      (:label option)])])

(defn update-task
  [event task-fsm]
  (let [el  (-> event (.-target))
        name (-> el (.-name) (keyword))
        value (-> el (.-value))
        data (case name
               :complete       {[:completed_at] (if (.-checked el)
                                                  (new js/Date)
                                                  nil)}
               {[name] value})]
    (fsm/dispatch task-fsm {:type :update :data data})))

(defn create-new-task-fsm
  []
  (let [fsm (ratom-fsm new-task-fsm-spec
                       {:id (str "new-task-fsm-" (js/Date.now))})
        parent-task-id @select/selected-task-id
        subscriptions [(fsm/subscribe fsm
                                      (fn [{:keys [action]}]
                                        (when (= (:type action) :created)
                                          (fsm/dispatch tasks-fsm :refresh))))

                       (router/sync-parent-id-from-route fsm)]]

    (when parent-task-id
      (js/setTimeout #(fsm/dispatch fsm {:type :update :data {[:parent_task_id] parent-task-id}}) 0))
    [fsm
     (fn []
       (for [unsubscribe subscriptions]
         (unsubscribe)))]))

(defn value
  [& validators]
  (apply v/-> (vjs/prop "value") validators))

(def form-data-validator
  (v/-> (vjs/prop "currentTarget")
        (vjs/prop "elements")
        (vjs/record
         {:title (value (v/string) (v/assert #(pos? (count %))))
          :due_date (value (v/union
                            (v/literal "")
                            (v/string->date)))
          :estimated_time (value (v/string->number))
          :parent_task_id (value (v/string))})))

(defn submit-form
  [event form-fsm]
  (.preventDefault event)
  (let [form-data (v/parse form-data-validator event)]
    (-> event .-currentTarget .-elements .-title .focus)
    (fsm/dispatch form-fsm {:type :submit :form-data form-data})))

(defn submit-form-on-enter
  [keyboard-event]
  (when (= (.-key keyboard-event) "Enter")
    (.preventDefault keyboard-event)
    (.stopImmediatePropagation keyboard-event)
    (.. keyboard-event -currentTarget requestSubmit)))

(defn new-task-form
  []
  (with-let [[form-fsm unsubscribe] (create-new-task-fsm)]
    (let [task (get form-fsm :task)
          selected-task-fsm @select/selected-task-fsm
          form-id "new-task-form"
          tasks (cons (:task selected-task-fsm)
                      @(select/child-tasks (get-in selected-task-fsm [:task :id])))]
      [:form
       {:id form-id
        :on-submit #(submit-form % form-fsm)
        :on-change #(update-task % form-fsm)
        :on-input #(update-task % form-fsm)
        :on-keydown submit-form-on-enter
        :class "flex flex-row px-2 gap-4 bg-stone-950/50 py-2 items-center rounded-lg mx-4"}

       [:div {:class "text-left flex-grow"}
        [title
         {:value (:title task)}]]

       [:div {}
        [estimate
         {:form-id form-id
          :value (:estimated_time task)}]]

       [:div {:class "min-w-32"}
        [due-date
         {:form-id form-id
          :value (date->string (:due_date task))}]]

       [:div {:class "text-sm"}
        [parent-task
         {:form-id form-id
          :options (tasks->options tasks @select/tasks-by-id)
          :value (:parent_task_id task)}]]])

    (finally
      (unsubscribe))))
