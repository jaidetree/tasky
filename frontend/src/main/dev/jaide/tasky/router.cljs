(ns dev.jaide.tasky.router
  (:require
   [clojure.string :as s]
   [cljs.pprint :refer [pprint]]
   [dev.jaide.finity.core :as fsm]
   [dev.jaide.tasky.dom :refer [on]]
   [dev.jaide.tasky.state-machines :refer [ratom-fsm]]
   [dev.jaide.valhalla.core :as v]))

(defn url->route
  [url]
  (let [parts (-> url
                  (subs 1)
                  (s/split #"/")
                  (vec))
        #_#_pattern (re-pattern "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")]
    (->> parts
         (partition 2)
         (map vec)
         #_(map (fn [[k v]]
                  [(keyword k) v]))
         (into {}))))

(comment
  (url->route (str "/tasks/" (random-uuid))))

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
                :context (if (empty? routes)
                           {:routes {"tasks" ""}}
                           {:routes routes})
                :effect :sync-popstate}))}

      {:from [:active]
       :actions [:pop :push]
       :to [:active]
       :do (fn sync [_state action]
             (let [routes (url->route (:url action))]
               {:state :active
                :context {:routes routes}
                :effect :sync-popstate}))}]}))

(def ^:private fsm (ratom-fsm router-fsm-spec))

(defn navigate
  [path-str]
  (js/window.history.pushState nil nil path-str)
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

(fsm/dispatch fsm {:type :init :url (location-str)})

#_(fsm/subscribe
   fsm
   (fn [{:keys [prev next action]}]
     (pprint action)))

(comment
  @router)
