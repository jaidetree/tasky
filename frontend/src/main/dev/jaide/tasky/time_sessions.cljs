(ns dev.jaide.tasky.time-sessions
  (:refer-clojure :exclude [update])
  (:require
   [promesa.core :as p]
   [dev.jaide.valhalla.core :as v]))

(def time-session
  (v/record
   {:id (v/string)
    :start_time (v/string->date {:accept-dates true})
    :end_time (v/nilable (v/string->date {:accept-dates true}))
    :original_end_time (v/nilable (v/string->date {:accept-dates true}))
    :description (v/string)
    :task_id (v/string)
    :interrupted_by_task_id (v/nilable (v/string))
    :duration_seconds (v/nilable (v/number))}))

(def time-sessions-map
  (v/hash-map
   (v/string)
   time-session))

(def time-sessions
  (v/record
   {:all time-sessions-map
    :order (v/vector (v/string))}))

(def time-sessions-list
  (v/vector time-session))

(defn create
  [session & {:keys [signal]}]
  (let [opts {:signal signal
              :method :POST
              :headers {"Content-Type" "application/json"}
              :body (-> {:time_session session}
                        (clj->js)
                        (js/JSON.stringify))}]
    (p/-> (js/fetch (str "/api/tasks/" (:task_id session) "/time_sessions")
                    (clj->js opts))
          (.json)
          (js->clj :keywordize-keys true))))

(defn update
  [session & {:keys [signal]}]
  (let [opts {:signal signal
              :method :PATCH
              :headers {"Content-Type" "application/json"}
              :body (-> {:time_session session}
                        (clj->js)
                        (js/JSON.stringify))}]
    (p/-> (js/fetch (str "/api/tasks/" (:task_id session) "/time_sessions/" (:id session))
                    (clj->js opts))
          (.json)
          (js->clj :keywordize-keys true))))

(defn delete
  [session & {:keys [signal]}]
  (let [opts {:signal signal
              :method :DELETE
              :headers {"Content-Type" "application/json"}}]
    (p/-> (js/fetch (str "/api/tasks/" (:task_id session) "/time_sessions/" (:id session)))
          (.json)
          (js->clj :keywordize-keys true))))

(comment)
