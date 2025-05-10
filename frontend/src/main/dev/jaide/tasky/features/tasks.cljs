(ns dev.jaide.tasky.features.tasks
  (:require
   [reagent.core :refer [atom class-names with-let]]
   [dev.jaide.finity.core :as fsm]
   [dev.jaide.valhalla.core :as v]
   [dev.jaide.valhalla.js :as vjs]
   [dev.jaide.tasky.router :as router]
   [dev.jaide.tasky.paths :as paths]
   [dev.jaide.tasky.state-machines :refer [ratom-fsm]]
   [dev.jaide.tasky.state.selectors :as select]
   [dev.jaide.tasky.state.app-fsm :refer [app-fsm]]
   [dev.jaide.tasky.state.task-fsm :refer [new-task-fsm-spec]]
   [dev.jaide.tasky.state.tasks-fsm :refer [all-tasks tasks-fsm]]
   [dev.jaide.tasky.views.delete-rocker :refer [delete-rocker]]
   [dev.jaide.tasky.views.task-form :as task-form]
   [dev.jaide.tasky.views.transition :as trans]
   ["@heroicons/react/24/outline" :refer [ChevronRightIcon ArrowUturnRightIcon]]))

(defn title
  [{:keys [value]}]
  [:input.flex-grow
   {:name "title"
    :class "text-sm w-full bg-slate-600/30 p-2 rounded"
    :value value
    :placeholder "New Task Title"}])

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
        :on-change #(task-form/update-task % form-fsm)
        :on-input #(task-form/update-task % form-fsm)
        :on-keydown submit-form-on-enter
        :class "flex flex-row px-2 gap-4 bg-stone-950/50 py-2 items-center rounded-lg mx-4"}

       [:div {:class "text-left flex-grow"}
        [title
         {:value (:title task)}]]

       [:div {}
        [task-form/estimate
         {:form-id form-id
          :value (:estimated_time task)}]]

       [:div {:class "min-w-32"}
        [task-form/due-date
         {:form-id form-id
          :value (task-form/date->string (:due_date task))}]]

       [:div {:class "text-sm"}
        [task-form/parent-task
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
          subtasks @(select/child-task-fsms (:id task))]
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
            :on-input #(task-form/update-task % task-fsm)}
           [task-form/checkbox
            {:checked (task-form/completed? task)}]]
          [:button.flex-grow.block.text-left
           {:type "button"
            :on-click #(router/navigate {"tasks" (:id task)})}
           (:title task)]]]

        [td {:class "w-50"}
         (task-form/min->str
          (:estimated_time task))]

        [td {:class "w-50"}
         (if-let [date (:due_date task)]
           (.toLocaleString date)
           "-")]

        [td {:class "w-50"}
         (:tracked_time task)]

        [td {:class ""}
         [:button
          {:class "cursor-pointer text-slate-500"
           :on-click #(router/navigate {"tasks" (:id task)})}
          [:> ArrowUturnRightIcon
           {:class "size-4"}]]]

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
       :on-click #(router/navigate {"tasks" ""})}
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
            :on-click #(router/navigate {"tasks" (:id task)})}
           (:title task)])])]))

(comment
  (or "" true))

(defn tasks-table
  [{:keys []}]
  (let [task-fsms @select/all-child-task-fsms]
    [:div
     [:table.min-w-full.table-auto
      [:thead
       {:class ""}
       [:tr
        [th
         {:class "rounded-l-lg"}
         "Name"]
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
         {}
         ""]
        [th
         {:class "rounded-r-lg"}
         ""]]]

      [:tbody
       (doall
        (for [task-fsm task-fsms]
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
  (let [selected-task-id @select/selected-task-id]
    [:div.px-8
     [:div
      [breadcrumbs
       {:task-id selected-task-id}]]
     [:section
      {:id "tasks-container"
       :class "space-y-4"}
      [tasks-table
       {}]]]))

