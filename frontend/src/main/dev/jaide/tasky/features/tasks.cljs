(ns dev.jaide.tasky.features.tasks
  (:require
   [reagent.core :refer [atom class-names with-let]]
   [dev.jaide.finity.core :as fsm]
   [dev.jaide.valhalla.core :as v]
   [dev.jaide.valhalla.js :as vjs]
   [dev.jaide.tasky.router :as router]
   [dev.jaide.tasky.paths :as paths]
   [dev.jaide.tasky.state-machines :refer [ratom-fsm]]
   [dev.jaide.tasky.state.app-fsm :refer [app-fsm]]
   [dev.jaide.tasky.state.task-fsm :refer [new-task-fsm-spec]]
   [dev.jaide.tasky.state.tasks-fsm :refer [all-tasks tasks-fsm]]
   [dev.jaide.tasky.views.delete-rocker :refer [delete-rocker]]
   [dev.jaide.tasky.views.transition :as trans]
   ["@heroicons/react/24/outline" :refer [ChevronRightIcon ArrowUturnRightIcon]]))

(defn estimate->map
  [minutes]
  (let [hours (js/Math.floor (/ minutes 60))
        minutes (- minutes (* hours 60))]
    [hours minutes]))

(defn update-task
  [task-fsm event]
  (let [name (-> event (.-target) (.-name) (keyword))
        value (-> event (.-target) (.-value))
        {:keys [completed_at]} (get task-fsm :task)
        data (case name
               :estimated_time_minutes
               {[:estimated_time_map :minutes] (js/Number value)}

               :estimated_time_hours
               {[:estimated_time_map :minutes] (js/Number value)}

               :estimated_time
               (let [value (js/Number value)
                     [hours minutes] (estimate->map value)]
                 {[:estimated_time] value
                  [:estimated_time_map :hours] hours
                  [:estimated_time_map :minutes] minutes})

               :complete {[:completed_at] (if (instance? js/Date completed_at)
                                            nil
                                            (new js/Date))}
               {[name] value})]
    (fsm/dispatch task-fsm {:type :update :data data})))

(defn title
  [{:keys [value]}]
  [:input.flex-grow
   {:name "title"
    :class "text-sm w-full bg-slate-600/30 p-2 rounded"
    :value value
    :placeholder "New Task Title"}])

(defn checkbox
  [{:keys [checked]}]
  [:input
   {:type "checkbox"
    :name "complete"
    :checked checked}])

(defn due-date
  [{:keys [form-id value]}]
  [:input
   {:form form-id
    :class "text-xs w-full border border-slate-700 p-2 rounded"
    :type "datetime-local"
    :name "due_date"
    :value value}])

(def estimates
  [["1 quarter" (* 3 30 24 60)]
   ["2 months" (* 2 30 24 60)]
   ["1 month" (* 30 24 60)]
   ["3 weeks" (* 3 7 24 60)]
   ["2 weeks" (* 2 7 24 60)]
   ["1 week" (* 7 24 60)]
   ["3 days" (* 3 24 60)]
   ["2 days" (* 2 24 60)]
   ["1 day" (* 24 60)]
   ["8 hr" (* 8 60)]
   ["6 hr" (* 6 60)]
   ["4 hr" (* 4 60)]
   ["3 hr" (* 3 60)]
   ["2 hr" (* 2 60)]
   ["1 hr" 60]
   ["45 min" 45]
   ["30 min" 30]
   ["20 min" 20]
   ["15 min" 15]
   ["10 min" 10]])

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

(defn estimated-time
  [{:keys [form-id value]}]
  [:select
   {:form form-id
    :class "text-sm border border-slate-700 b-2 rounded p-2"
    :name "estimated_time"
    :value (js/String value)}
   [:option
    {:value "0"}
    "-- unknown --"]
   (for [[label min] estimates]
     [:option
      {:key min
       :value min}
      label])])

(defn parent-task
  [{:keys [form-id value tasks]}]
  [:select
   {:name "parent_task_id"
    :class "text-sm border border-slate-700 b-2 rounded p-2 w-[18.75rem]"
    :value value
    :form form-id}
   [:option
    {:value ""}
    "-- No Parent Task --"]
   (doall
    (for [task tasks]
      [:option
       {:key (:id task)
        :value (:id task)}
       (:title task)]))])

(defn create-new-task-fsm
  []
  (let [fsm (ratom-fsm new-task-fsm-spec
                       {:id (str "new-task-fsm-" (js/Date.now))})
        parent-task-id (get-in (router/route) [:paths 0] "")
        subscriptions [(fsm/subscribe fsm
                                      (fn [{:keys [action]}]
                                        (when (= (:type action) :created)
                                          (fsm/dispatch tasks-fsm :refresh))))

                       (router/sync-parent-id-from-route fsm)]]

    (js/setTimeout #(fsm/dispatch fsm {:type :update :data {[:parent_task_id] parent-task-id}}) 0)
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
  [form-fsm event]
  (.preventDefault event)
  (let [form-data (v/parse form-data-validator event)
        [hours minutes] (estimate->map (:estimated_time form-data))
        form-data (merge form-data
                         {:estimated_time_map {:hours hours
                                               :minutes minutes}})]
    (-> event .-currentTarget .-elements .-title .focus)
    (fsm/dispatch form-fsm {:type :submit :form-data form-data})))

(defn submit-form-on-enter
  [keyboard-event]
  (when (= (.-key keyboard-event) "Enter")
    (.preventDefault keyboard-event)
    (.stopImmediatePropagation keyboard-event)
    (.. keyboard-event -currentTarget requestSubmit)))

(defn date->string
  [date]
  (if (instance? js/Date date)
    (let [year (.getFullYear date)
          month (-> date .getMonth .toString (.padStart 2 "0"))
          day (-> date .getDate .toString (.padStart 2 "0"))
          hour (-> date .getHours .toString (.padStart 2 "0"))
          min (-> date .getMinutes .toString (.padStart 2 "0"))]
      (str year "-" month "-" day "T" hour ":" min))
    ""))

(defn new-task-form
  []
  (with-let [[form-fsm unsubscribe] (create-new-task-fsm)]
    (let [task (get form-fsm :task)
          selected-task-id (get-in (router/route) [:paths 0] "")
          form-id (str "new-task-form-" (:id task))
          tasks (->> (get tasks-fsm :tasks)
                     (keep #(let [task-fsm (get-in % [:fsm])]
                              (when (and
                                     (or (= (get-in task-fsm [:task :parent_task_id])
                                            selected-task-id)
                                         (= (get-in task-fsm [:task :id])
                                            selected-task-id))
                                     (not= (:state @task-fsm) :deleted))
                                (get task-fsm :task)))))]
      [:form
       {:id form-id
        :on-submit #(submit-form form-fsm %)
        :on-change #(update-task form-fsm %)
        :on-input #(update-task form-fsm %)
        :on-keydown submit-form-on-enter
        :class "flex flex-row px-2 gap-4 bg-stone-950/50 py-2 items-center rounded-lg mx-4"}

       [:div {:class "text-left flex-grow"}
        [title
         {:value (:title task)}]]

       [:div {}
        [estimated-time
         {:form-id form-id
          :value (:estimated_time task)}]]

       [:div {:class "min-w-32"}
        [due-date
         {:form-id form-id
          :value (date->string (:due_date task))}]]

       [:div {:class "text-sm"}
        [parent-task
         {:form-id form-id
          :tasks tasks
          :value (:parent_task_id task)}]]])

    (finally
      (unsubscribe))))

(defn th
  [{:as attrs} & children]
  (into
   [:th.py-3.px-4.text-left.bg-gray-200
    (merge attrs
           {:class (class-names "bg-gray-200 bg-slate-600" (:class attrs))})]
   children))

(defn td
  [{:as attrs} & children]
  (into
   [:td.py-2.px-4.text-sm
    (or attrs {})]
   children))

(defn task-row
  [{:keys [task-fsm tasks level]
    :or {level 0}}]
  (with-let [complete (instance? js/Date (get-in task-fsm [:task :completed_at]))
             toggle-atom (atom (not complete))]
    (let [{:keys [state context]} @task-fsm
          task (:task context)
          subtasks (->> (get tasks-fsm :tasks)
                        (map :fsm)
                        (filter #(= (get-in % [:task :parent_task_id])
                                    (:id task))))]
      [:<>
       [:tr
        {:class (trans/class
                 {:active (= state :deleting)
                  :enter "transition-opacity duration-1000 bg-red-500/50 ease-in-out"
                  :from "opacity-100"
                  :to "opacity-0"})
         :on-transitionend #(fsm/dispatch task-fsm :deleted)}

        [td {:class "text-left"}
         [:div.flex.flex-row.flex-nowrap.gap-2.relative
          {:style {:marginLeft (str level "rem")}}
          [:div
           {:class "absolute right-full top-0 h-full flex flex-row items-center gap-2 mr-2"}
           (when (> (count subtasks) 0)
             [:button
              {:type "button"
               :class ""
               :on-click #(swap! toggle-atom not)}
              [:> ChevronRightIcon
               {:class (class-names "size-4 transition-transform duration-200 ease-in-out"
                                    (when @toggle-atom
                                      "rotate-90"))}]])]
          [:form
           {:id (str "task-form-" (:id task))
            :on-submit #(do (.preventDefault %)
                            (fsm/dispatch task-fsm {:type :save}))
            :on-input #(update-task task-fsm %)}
           [checkbox
            {:checked (some? (:completed_at task))}]]
          [:button.flex-grow.block.text-left
           {:type "button"
            :on-click #(router/navigate (str "/tasks/" (:id task)))}
           (:title task)]]]

        [td {:class ""}
         [:button
          {:class "cursor-pointer text-slate-500"
           :on-click #(router/navigate (str "/tasks/" (:id task)))}
          [:> ArrowUturnRightIcon
           {:class "size-4"}]]]

        [td {:class "w-50"}
         (min->str
          (:estimated_time task))]

        [td {:class "w-50"}
         (if-let [date (:due_date task)]
           (.toLocaleString date)
           "-")]

        [td {:class "w-50"}
         (:tracked_time task)]

        [td {:class "w-32"}
         [:div
          {:class "flex flex-row justify-end"}
          (when-not (= (:id task) "")
            [delete-rocker
             {:id (str "task-" (:id task))
              :on-delete #(fsm/dispatch task-fsm {:type :delete})}])]]]

       (when @toggle-atom
         (doall
          (for [task-fsm subtasks]
            [task-row
             {:key (:id task)
              :task-fsm task-fsm
              :tasks tasks
              :level (inc level)}])))])
    (finally
      #_(fsm/destroy tr-fsm))))

(defn collect-parents
  [task-id all-tasks]
  (vec
   (loop [parents []
          task-id task-id
          tasks all-tasks]
     (let [[task & tasks] tasks]
       (cond
         (nil? task)
         parents

         (= (:id task) task-id)
         (recur
          (cons task parents)
          (:parent_task_id task)
          all-tasks)

         :else
         (recur
          parents
          task-id
          tasks))))))

(defn breadcrumbs
  [{:keys [task-id]}]
  (let [tasks (collect-parents task-id (all-tasks))
        last-task (last tasks)]
    [:div.mb-4
     [:button
      {:class "text-blue-500 cursor-pointer"
       :on-click #(router/navigate "/")}
      "All Tasks"]
     (for [task tasks]
       [:<>
        {:key (:id task)}
        [:span
         {:class "inline-block mx-2 text-secondary"}
         "/"]
        (if (= task last-task)
          [:span
           (:title task)]
          [:button
           {:type "button"
            :class "text-blue-500 cursor-pointer"
            :on-click #(router/navigate (str "/tasks/" (:id task)))}
           (:title task)])])]))

(defn tasks-table
  [{:keys []}]
  (let [selected-task-id (get-in (router/route) [:paths 0])
        root-fsms (->> (get-in tasks-fsm [:tasks])
                       (map :fsm)
                       (filter #(let [parent-id (get-in % [:task :parent_task_id])]
                                  (= parent-id selected-task-id))))]
    [:div.overflow-x-auto.px-8
     [breadcrumbs
      {:task-id selected-task-id}]
     [:table.min-w-full.table-auto
      [:thead
       {:class ""}
       [:tr
        [th
         {:class "rounded-l-lg"}
         "Name"]
        [th
         {}
         ""]
        [th
         {}
         "Estimate"]
        [th
         {}
         "Due"]
        [th
         {}
         "Elapsed"]
        [th
         {:class "rounded-r-lg"}
         ""]]]

      [:tbody
       (doall
        (for [task-fsm root-fsms]
          (let [task (get task-fsm :task)]
            [task-row {:key (:id task)
                       :task-fsm task-fsm
                       :tasks-fsm tasks-fsm}])))]]
     [new-task-form
      {}]]))

(defn new-task
  []
  (fsm/dispatch app-fsm {:type :new-task}))

(defn tasks-index
  [{:keys []}]
  [:secton
   {:id "tasks-container"
    :class "space-y-4"}
   [:header
    {:class "flex flex-row justify-between items-end px-8"}
    [:h1
     {:class "text-2xl font-bold"}
     "Tasks"]
    [:div
     {:class "inline-flex flex-row gap-2 justify-end"}
     [:button
      {:type "button"
       :class "btn bg-blue-500"
       :on-click new-task}
      "New Task"]]]
   [tasks-table
    {}]])

