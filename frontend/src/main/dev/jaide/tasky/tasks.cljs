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
    :description (v/string)
    :interrupted_by_task_id (v/nilable (v/string))}))

(def task-validator
  (v/record
   {:completed_at (v/nilable
                   (v/string->date
                    {:accept-dates true}))
    :created_at (v/string->date {:accept-dates true})
    :due_date (v/nilable
               (v/union
                (v/literal "")
                (v/string->date
                 {:accept-dates true})))
    :estimated_time (v/nilable (v/string->number {:accept-numbers true}))
    :id (v/string)
    :description (v/default (v/string) "")
    :parent_task_id (v/nilable (v/string))
    :title (v/string)
    :time_sessions (v/vector session-validator)
    :tracked_time (v/number)
    :updated_at (v/union
                 (v/string->date)
                 (v/date))}))

(def draft-validator
  (v/record
   {:title (v/string)
    :estimated_time (v/nilable
                     (v/string->number {:accept-numbers true}))
    :description (v/nilable
                  (v/string))
    :due_date (v/nilable
               (v/union
                (v/literal "")
                (v/string->date
                 {:accept-dates true})))
    :parent_task_id (v/nilable (v/string))}))

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

(defn create-task
  [task & {:keys [signal]}]
  (let [opts {:signal signal
              :method :POST
              :headers {"Content-Type" "application/json"}
              :body (-> {:task task}
                        (clj->js)
                        (js/JSON.stringify))}]
    (p/-> (js/fetch "/api/tasks"
                    (clj->js opts))
          (.json)
          (js->clj :keywordize-keys true))))

(defn update-task
  [task & {:keys [signal]}]
  (let [opts {:signal signal
              :method :PUT
              :headers {"Content-Type" "application/json"}
              :body (-> {:task task}
                        (clj->js)
                        (js/JSON.stringify))}]
    (p/-> (js/fetch (str "/api/tasks/" (:id task))
                    (clj->js opts))
          (.json)
          (js->clj :keywordize-keys true))))

(defn delete-task
  [task-id]
  (let [opts {:method :DELETE
              :headers {"Content-Type" "application/json"}
              :body "{}"}]
    (js/fetch (str "/api/tasks/" task-id)
              (clj->js opts))))
