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
  (let [[route id & remaining] paths]
    (if (= route "task")
      (assoc routes route id :_ [])
      routes)))

(defn new-route
  "Handles creating a new task in the sidebar"
  [{paths :_ :as routes}]
  (let [[route & _remaining] paths]
    (if (= route "new")
      (assoc routes route "" :_ [])
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

(def router-fsm-spec
  (fsm/define
    {:id :router

     :initial {:state :inactive
               :context {}}

     :states {:inactive {}
              :active {:routes (v/hash-map (v/string) (v/string))}}

     :actions {:init {:url (v/string)}
               :pop {:url (v/string)}
               :push {:url (v/string)}}

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
             (let [routes (url->route (:url action))]
               {:state :active
                :context {:routes (merge
                                   (get-in state [:context :routes])
                                   routes)}
                :effect :sync-popstate}))}

      {:from [:active]
       :actions [:pop]
       :to [:active]
       :do (fn sync [_state action]
             (let [routes (url->route (:url action))]
               {:state :active
                :context {:routes routes}
                :effect :sync-popstate}))}]}))

(def ^:private fsm (ratom-fsm router-fsm-spec))

(defn navigate
  [path-str]
  (set! (.-scrollTop js/document.documentElement) 0)
  (fsm/dispatch fsm {:type :push :url path-str}))

(defn get-selected-task-id
  []
  (get-in fsm [:routes "tasks"]))

(defn sync-parent-id-from-route
  [form-fsm]
  (fsm/subscribe
   fsm
   (fn [{:keys [prev next _action]}]
     (let [task-id (get-in next [:context :paths "tasks"] "")]
       (when (not= (:context prev) (:context next))
         (fsm/dispatch form-fsm {:type :update
                                 :data {[:parent_task_id] task-id}}))))))

(defn routes
  []
  (get fsm :routes))

(fsm/subscribe
 fsm
 (fn [{:keys [next action]}]
   (when (= (:type action) :push)
     (let [routes (get-in next [:context :routes])
           path-str (->> ["tasks" "task" "new"]
                         (reduce
                          (fn [s prefix]
                            (let [value (get routes prefix)]
                              (cond
                                (nil? value) s
                                (s/blank? value) (str s prefix)
                                :else (s prefix "/" value))))))]

       (js/window.history.pushState nil nil path-str)))))

(fsm/dispatch fsm {:type :init :url (location-str)})

#_(fsm/subscribe
   fsm
   (fn [{:keys [prev next action]}]
     (pprint action)))

(comment
  @router)
