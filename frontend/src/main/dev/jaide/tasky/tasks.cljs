(ns dev.jaide.tasky.tasks
  (:require
   [promesa.core :as p]
   [dev.jaide.valhalla.core :as v]
   [dev.jaide.tasky.utils :refer [map->query-string]]))

(def session-validator
  (v/record
   {:id (v/string)
    :start_time (v/string->date {:accept-dates true})
    :end_time (v/nilable (v/string->date {:accept-dates true}))
    :notes (v/string)
    :interrupted_by_task_id (v/nilable (v/string))}))

(def task-validator
  (v/record
   {:completed_at (v/nilable
                   (v/string->date
                    {:accept-dates true}))
    :created_at (v/string->date {:accept-dates true})
    :due_date (v/nilable
               (v/string->date
                {:accept-dates true}))
    :estimated_time (v/nilable (v/number))
    :estimated_time_map (v/record {:minutes (v/number)
                                   :hours (v/number)})
    :id (v/string)
    :notes (v/string)
    :parent_task_id (v/nilable (v/string))
    :title (v/string)
    :time_sessions (v/vector session-validator)
    :tracked_time (v/number)
    :updated_at (v/union
                 (v/string->date)
                 (v/date))}))

(def tasks-validator (v/vector task-validator))

(defn fetch-tasks
  [& {:keys [task-id]}]
  (p/-> (js/fetch (str "/api/tasks" (if task-id (map->query-string {:id task-id}) "")))
        (.json)
        (js->clj :keywordize-keys true)))

(defn fetch-task
  [{:keys [task-id]}]
  (p/-> (js/fetch (str "/api/tasks/" task-id))
        (.json)
        (js->clj :keywordize-keys true)))
