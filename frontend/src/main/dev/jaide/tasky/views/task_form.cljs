(ns dev.jaide.tasky.views.task-form
  (:require
   [dev.jaide.finity.core :as fsm]))

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

(defn parent-task
  [{:keys [form-id value tasks]}]
  [:select
   {:name "parent_task_id"
    :class "text-sm border border-slate-700 b-2 rounded p-2 w-full"
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
