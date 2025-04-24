(ns dev.jaide.tasky.features.tasks
  (:require
   [clojure.string :as s]
   [reagent.core :refer [class-names with-let]]
   [dev.jaide.finity.core :as fsm]
   [dev.jaide.tasky.state-machines :refer [ratom-fsm]]
   [dev.jaide.tasky.state.app-fsm :refer [app-fsm]]
   [dev.jaide.tasky.state.task-fsm :refer [blank-task new-task-fsm-spec]]
   [dev.jaide.tasky.state.tasks-fsm :refer [tasks-fsm]]
   [dev.jaide.tasky.views.delete-rocker :refer [delete-rocker]]))

(defn th
  [& children]
  (into
   [:th.py-3.px-4.text-left]
   children))

(defn td
  [{:as attrs} & children]
  (into
   [:td.py-2.px-4.text-sm
    (or attrs {})]
   children))

(defn update-task
  [task-fsm event]
  (let [name (-> event (.-target) (.-name) (keyword))
        value (-> event (.-target) (.-value))
        data (case name
               :estimated_time_minutes
               {[:estimated_time_map :minutes] (js/Number value)}

               :estimated_time_hours
               {[:estimated_time_map :minutes] (js/Number value)}

               :estimated_time
               (let [value (js/Number value)
                     hours (js/Math.floor (/ value 60))
                     minutes (- value (* hours 60))]
                 {[:estimated_time] value
                  [:estimated_time_map :hours] hours
                  [:estimated_time_map :minutes] minutes})

               :complete {[:completed_at] (new js/Date)}
               {[name] value})]
    (fsm/dispatch task-fsm {:type :update :data data})))

(defn title
  [{:keys [value]}]
  [:input.flex-grow
   {:name "title"
    :class "text-sm"
    :value value}])

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
    :class "text-xs"
    :type "datetime-local"
    :name "due_date"
    :value value}])

;; @TODO Just generate a list of options and find the matching one
(def units
  [["quarter" (* 3 30 24 60)]
   ["month" (* 30 24 60)]
   ["week" (* 7 24 60)]
   ["day" (* 24 60)]
   ["hr" 60]
   ["min" 0]])

(defn- min->str
  [minutes]
  (let [minutes (js/Number minutes)]
    (loop [minutes minutes
           units units
           values []]
      (let [[[label unit] & units] units]
        (cond
          (or (= minutes 0)
              (nil? unit))
          (s/join ", " values)

          (= minutes unit)
          (recur 0
                 []
                 (conj values (str (/ minutes unit) " " label)))

          (> minutes unit)
          (let [base (if (< unit 60)
                       minutes
                       (js/Math.floor (/ minutes unit)))
                remaining (- minutes (* base unit))]
            (recur remaining
                   units
                   (conj values (str base " " label (if (> 1 base) "s" "")))))

          :else
          (recur minutes
                 units
                 values))))))

(comment
  (min->str (* 8 24 60)))

(defn estimated-time
  [{:keys [form-id value]}]
  (println "estimated-time" (pr-str value))
  [:select
   {:form form-id
    :class "text-sm"
    :name "estimated_time"
    :value (if (zero? value) "" (js/String value))}
   [:option
    {:value ""}
    "-- unknown --"]
   (for [min [10 15 20 30 45 60]]
     [:option
      {:key min
       :value min}
      (str min " min")])
   (for [min (range 60 (* 9 60) 60)]
     [:option
      {:key min
       :value min}
      (min->str min)])
   (for [[label min] [["1 day" (* 24 60)]
                      ["1 weeks" (* 7 24 60)]
                      ["2 weeks" (* 2 7 24 60)]
                      ["3 weeks" (* 3 7 24 60)]
                      ["1 month" (* 30 24 60)]
                      ["2 months" (* 2 30 24 60)]
                      ["1 quarter" (* 2 30 24 60)]]]
     [:option
      {:key min
       :value min}
      label])])

(defn create-new-task-fsm
  []
  (let [fsm (ratom-fsm new-task-fsm-spec
                       {:id (str "new-task-fsm-" (js/Date.now))})]
    [fsm
     (fsm/subscribe fsm
                    (fn [{:keys [action]}]
                      (when (= (:type action) :created)
                        (fsm/dispatch tasks-fsm :refresh))))]))

(defn task-row
  [{:keys [task-fsm tasks level]
    :or {level 0}}]
  (with-let [unsubscribe (fsm/subscribe task-fsm
                                        (fn [{:keys [action]}]
                                          (when (= (:type action) :deleted)
                                            (fsm/dispatch tasks-fsm {:type :refresh}))))]

    (let [task (get task-fsm :task)
          subtasks (->> (get tasks-fsm :tasks)
                        (filter #(and (= (get-in % [:task :parent_task_id])
                                         (:id task))
                                      (not= (:state @%) :deleted))))]
      [:<>
       [:tr
        {:class (class-names
                 "transition transition-opacity duration-500 ease-in-out"
                 (if (= (:state @task-fsm) :deleting)
                   "bg-red-500/25 opacity-0"
                   "opacity-100"))}
        [td {:class "text-left"}
         [:div.flex.flex-row.flex-nowrap.gap-2
          {:style {:paddingLeft (str level "rem")}}
          [:form
           {:id (str "task-form-" (:id task))
            :on-submit #(do (.preventDefault %)
                            (fsm/dispatch task-fsm {:type :save}))
            :on-input #(update-task task-fsm %)}
           [checkbox
            {:checked (some? (:completed_at task))}]]
          [:span
           (:title task)]]]
        [td {}
         (if-let [date (:due_date task)]
           (.toLocaleString date)
           "-")]
        [td {}
         (min->str
          (:estimated_time task))]
        [td {}
         (:tracked_time task)]
        [td {}
         [:div
          {:class "flex flex-row justify-end"}
          (when-not (= (:id task) "")
            [delete-rocker
             {:id (str "task-" (:id task))
              :on-delete #(fsm/dispatch task-fsm {:type :delete})}])]]]
       (doall
        (for [task-fsm subtasks]
          [task-row
           {:key (:id task)
            :task-fsm task-fsm
            :tasks tasks
            :level (inc level)}]))])
    (finally
      (unsubscribe))))

(defn new-task-row
  [{:keys [level]
    :or {level 0}}]
  (with-let [[form-fsm unsubscribe] (create-new-task-fsm)]
    (let [task (get form-fsm :task)
          form-id (str "new-task-form-" (:id task))
          tasks (->> (get tasks-fsm :tasks)
                     (map #(get % :task)))]
      [:<>
       [:tr
        {:class (class-names
                 "transition transition-opacity duration-500 ease-in-out"
                 (if (= (:state @form-fsm) :deleting)
                   "bg-red-500/25 opacity-0"
                   "opacity-100"))
         :on-change #(update-task form-fsm %)}

        [td {:class "text-left"}
         [:div
          {:style {:paddingLeft (str level "rem")}}
          [:form.flex.flex-row.flex-nowrap.gap-2
           {:id form-id
            :on-submit #(do (.preventDefault %)
                            (fsm/dispatch form-fsm {:type :save}))
            :on-input #(update-task form-fsm %)}
           [checkbox
            {:checked (some? (:completed_at task))}]
           [title
            {:value (:title task)}]]]]
        [td {}
         [due-date
          {:form-id form-id
           :value (if-let [date (:due_date task)]
                    (.toISOString date)
                    "")}]]
        [td {}
         [estimated-time
          {:form-id form-id
           :value (:estimated_time task)}]]
        [td {:class "text-sm"
             :col-span 2}
         [:select.w-full
          {:name "parent_task_id"
           :value (:parent_task_id task)}
          [:option
           {:value ""}
           "-- No Parent Task --"]
          (doall
           (for [task tasks]
             [:option
              {:key (:id task)
               :value (:id task)}
              (:title task)]))]]]])

    (finally
      (unsubscribe))))

(defn tasks-table
  [{:keys []}]
  (let [root-fsms (->> (get-in tasks-fsm [:tasks])
                       (filter #(and (nil? (get-in % [:task :parent_task_id]))
                                     (not= (:state @%) :deleted))))]
    [:div.overflow-x-auto.shadow-md.rounded-lg
     [:table.min-w-full.table-auto
      [:thead
       {:class "bg-gray100 dark:bg-slate-600"}
       [:tr
        [th "Name"]
        [th "Due"]
        [th "Estimate"]
        [th "Elapsed"]
        [th ""]]]

      [:tbody
       (doall
        (for [task-fsm root-fsms]
          (let [task (get task-fsm :task)]
            [task-row {:key (:id task)
                       :task-fsm task-fsm
                       :tasks-fsm tasks-fsm}])))]
      [:tfoot
       [new-task-row
        {}]]]]))

(defn new-task
  []
  (fsm/dispatch app-fsm {:type :new-task}))

(defn tasks-index
  [{:keys []}]
  [:secton
   {:id "tasks-container"
    :class "space-y-4"}
   [:header
    {:class "flex flex-row justify-between items-end"}
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

(comment
  (let [fsm @debug-atom]
    (fsm/dispatch fsm {:type :update
                       :data {[:title] "Uxternal update"}})))
