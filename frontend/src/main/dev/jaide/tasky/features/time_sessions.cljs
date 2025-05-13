(ns dev.jaide.tasky.features.time-sessions
  (:require
   [clojure.string :as s]
   [dev.jaide.finity.core :as fsm]
   [dev.jaide.tasky.features.toaster :refer [toast dismiss]]
   [dev.jaide.tasky.state.time-sessions-fsm :refer [time-sessions-fsm clock-in clock-out]]
   [dev.jaide.tasky.time-sessions :as time-sessions]
   [dev.jaide.tasky.state.selectors :as select]
   [dev.jaide.tasky.views.table :refer [th td]]))

(defn parse-duration
  [duration]
  (let [remaining duration
        days (js/Math.floor (/ remaining (* 24 60 60)))
        remaining (- remaining (* days 24 60 60))
        hours (js/Math.floor (/ remaining (* 60 60)))
        remaining (- remaining (* hours 60 60))
        minutes (js/Math.floor (/ remaining 60))
        remaining (- remaining (* minutes 60))
        seconds remaining]
    {:days days
     :hours hours
     :minutes minutes
     :seconds seconds}))

(defn sec->str
  [duration]
  (let [{:keys [days hours minutes seconds]} (parse-duration duration)]
    (->> [[days "d"]
          [hours "hrs"]
          [minutes "min"]
          [seconds "s"]]
         (filter #(or (pos? (first %))
                      (= (second %) "s")))
         (map #(str (first %) " " (second %)))
         (s/join " "))))

(defn min->sec
  [duration]
  (* duration 60))

(defn time-session-row
  [{:keys [time-session]}]
  [:tr
   [td
    {}
    (.toLocaleString (:start_time time-session))]
   [td
    {}
    (when-let [end-time (:end_time time-session)]
      (.toLocaleString end-time))]
   [td
    {}
    (sec->str (:duration_seconds time-session))]
   [td
    {}
    (:interrupted_by_task_id time-session)]])

(defn time-sessions-table
  []
  (let [task-fsm @select/selected-task-fsm
        task (:task task-fsm)]
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
       (for [session-id (get-in task-fsm [:sessions :order])]
         (let [time-session (get-in task-fsm [:sessions :all session-id])]
           [time-session-row {:key (:id time-session)
                              :time-session time-session}])))]]))

(defn clock-actions
  []
  (let [task-id @select/selected-task-id]
    [:div.flex.flex-row.gap-4
     [:button
      {:class "btn bg-red-500"}
      "Clock Out"]
     [:button
      {:class "btn bg-blue-500"
       :on-click #(clock-in task-id)}
      "Clock In"]]))

(defn task-clock
  [{:keys [task-fsm]}]
  (let [task (get task-fsm :task)
        elapsed (get task-fsm :elapsed)
        estimated (-> (get task :estimated_time)
                      (min->sec))]
    [:div
     [:div
      [:span
       {:class "block text-slate-400/80 text-xs uppercase tracking-wide"}
       "Task"]

      [:h2.text-lg
       (:title task)]]
     [:div
      {:class "flex flex-row justify-between mt-4 items-center"}
      [:div
       {:class "font-mono flex flex-row justify-start items-end gap-2"}
       [:div
        [:span
         {:class "block text-slate-400/80 text-xs/5 uppercase tracking-wide"}
         "Elapsed"]
        (sec->str elapsed)]
       [:span
        {:class "text-base text-slate-400/80"}
        " / "]
       [:div
        [:span
         {:class "block text-slate-400/80 text-xs/5 uppercase tracking-wide"}
         "Estimate"]
        (sec->str estimated)]]
      [:button
       {:on-click clock-out
        :type "button"
        :class "btn bg-red-500"}
       "Clock Out"]]]))

(fsm/subscribe
 time-sessions-fsm
 (fn [{:keys [next action]}]
   (when (= (:state next) :clocked-in)
     (let [task-fsm (get-in next [:context :task-fsm])]
       (toast
        {:id "active-task"
         :type :info
         :duration nil
         :dismissable false
         :title "Clocked In"
         :content [task-clock
                   {:task-fsm task-fsm}]})))
   (when (and (= (:state next) :idle)
              (= (:type action) :clock-out))
     (dismiss "active-task"))))


