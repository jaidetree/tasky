(ns dev.jaide.tasky.router
  (:require
   [clojure.string :as s]
   [cljs.pprint :refer [pprint]]
   [dev.jaide.finity.core :as fsm]
   [dev.jaide.tasky.dom :refer [on]]
   [dev.jaide.tasky.state-machines :refer [ratom-fsm]]
   [dev.jaide.valhalla.core :as v]))

(defn tasks-route
  "Handles navigating hierarchical tasks"
  [{paths :_ :as routes}]
  (let [[route id & remaining] paths]
    (if (= route "tasks")
      (assoc routes route id :_ remaining)
      routes)))

(defn task-route
  "Handles viewing a task in the sidebar"
  [{paths :_ :as routes}]
  (let [[route id & _remaining] paths]
    (if (= route "task")
      (-> routes
          (assoc-in [:sidebar route] id)
          (assoc :_ []))
      routes)))

(defn new-route
  "Handles creating a new task in the sidebar"
  [{paths :_ :as routes}]
  (let [[route & _remaining] paths]
    (if (= route "new")
      (-> routes
          (assoc-in [:sidebar route] "")
          (assoc :_ []))
      routes)))

(def route-parsers [tasks-route
                    task-route
                    new-route])

(defn parse-routes
  [paths]
  (loop [routes {:_ paths}
         parsers route-parsers]
    (let [[parser & parsers] parsers
          routes (parser routes)]
      (cond
        (or (empty? (:_ routes))
            (empty? parsers))
        (assoc routes :_
               (if (nil? (:_ routes))
                 []
                 (vec (:_ routes))))

        :else
        (recur routes
               parsers)))))

(comment
  (parse-routes ["tasks" "xxxyyyzzz"])
  (parse-routes ["tasks" "xxxyyyzzz" "task" "aaabbbccc"])
  (parse-routes ["tasks" "xxxyyyzzz" "task" "aaabbbccc" "new"])
  (parse-routes ["tasks" "xxxyyyzzz" "new" "task" "aaabbbccc"])
  (parse-routes ["tasks" "xxxyyyzzz" "new"]))

(defn url->route
  [url]
  (let [paths (s/split (subs url 1) #"/")]
    (parse-routes paths)))

(defn location-str
  []
  (js/window.location.pathname.toString))

(def url-param-validator (v/string))

(def common-validators
  {"tasks" (v/nilable url-param-validator)})

(def router-fsm-spec
  (fsm/define
    {:id :router

     :initial {:state :inactive
               :context {}}

     :states {:inactive {}
              :active {:routes
                       (v/record
                        {"tasks" (v/nilable (v/string))
                         :sidebar (v/nilable
                                   (v/union
                                    (v/literal {})
                                    (v/record {"task" (v/string)})
                                    (v/record {"new" (v/literal "")})))})}}

     :actions {:init {:url (v/string)}
               :pop {:url (v/string)}
               :push {:route (v/nilable (v/assert map?))
                      :replace (v/nilable (v/keyword))}}

     :effects {:sync-popstate [{}
                               (fn [{:keys [dispatch]}]
                                 (on js/window "popstate"
                                     #(do
                                        (.preventDefault %)
                                        (dispatch {:type :pop :url (location-str)}))))]}

     :transitions
     [{:from [:inactive]
       :actions [:init]
       :to [:active]
       :do (fn init [_state action]
             (let [routes (url->route (:url action))]
               {:state :active
                :context (if (empty? (dissoc routes :_))
                           {:routes {"tasks" ""}}
                           {:routes routes})
                :effect :sync-popstate}))}

      {:from [:active]
       :actions [:push]
       :to [:active]
       :do (fn sync [state action]
             (let [{:keys [route replace]} action
                   routes (get-in state [:context :routes])]
               {:state :active
                :context {:routes (doto (if replace
                                          (-> routes
                                              (assoc replace route))
                                          (merge routes route)))}
                :effect :sync-popstate}))}

      {:from [:active]
       :actions [:pop]
       :to [:active]
       :do (fn sync [_state action]
             (let [routes (url->route (:url action))]
               {:state :active
                :context {:routes routes}
                :effect :sync-popstate}))}]}))

(def router-fsm (ratom-fsm router-fsm-spec))

(defn navigate
  [route & {:keys [replace]}]
  (fsm/dispatch router-fsm {:type :push :route route :replace replace}))

(comment)

(defn sync-parent-id-from-route
  [form-fsm]
  (fsm/subscribe
   router-fsm
   (fn [{:keys [prev next _action]}]
     (let [task-id (get-in next [:context :paths "tasks"] "")]
       (when (not= (:context prev) (:context next))
         (fsm/dispatch form-fsm {:type :update
                                 :data {[:parent_task_id] task-id}}))))))
(defn routes
  []
  (get router-fsm :routes))

(fsm/subscribe
 router-fsm
 (fn [{:keys [next action]}]
   (when (= (:type action) :push)
     (let [routes (get-in next [:context :routes])
           path-str (->> [["tasks"] [:sidebar "task"] [:sidebar "new"]]
                         (keep
                          (fn [path]
                            (let [value (get-in routes path)
                                  prefix (last path)]
                              (when (and value (not (s/blank? value)))
                                (str prefix "/" value)))))
                         (s/join "/")
                         (str "/"))]

       (set! (.-scrollTop js/document.documentElement) 0)
       (js/window.history.pushState nil nil path-str)))))

#_(fsm/subscribe
   fsm
   (fn [{:keys [next action]}]
     (pprint {:action action
              :state next})))

(fsm/dispatch router-fsm {:type :init :url (location-str)})

(comment
  @router)
