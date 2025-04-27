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
  (let [[route & paths] (s/split (subs url 1) #"/")
        route (if (s/blank? route)
                :tasks
                (keyword route))]
    [route (vec paths)]))

(defn location-str
  []
  (js/window.location.pathname.toString))

(def router-fsm-spec
  (fsm/define
    {:id :router

     :initial {:state :inactive
               :context {}}

     :states {:inactive {}
              :active {:route (v/keyword)
                       :paths (v/vector (v/string))}}

     :actions {:pop {:url (v/string)}
               :push {:url (v/string)}}

     :effects {:sync-popstate [{}
                               (fn [{:keys [dispatch]}]
                                 (on js/window "popstate"
                                     #(do
                                        (.preventDefault %)
                                        (dispatch {:type :pop :url (location-str)}))))]}

     :transitions
     [{:from [:inactive]
       :actions [:fsm/create]
       :to [:active]
       :do (fn init [_state _action]
             (let [[route paths] (url->route (location-str))]
               {:state :active
                :context {:route route
                          :paths paths}
                :effect :sync-popstate}))}

      {:from [:active]
       :actions [:pop :push]
       :to [:active]
       :do (fn sync [_state action]
             (let [[route paths] (url->route (:url action))]
               {:state :active
                :context {:route route
                          :paths paths}
                :effect :sync-popstate}))}]}))

(def ^:private fsm (ratom-fsm router-fsm-spec))

(defn navigate
  [path-str]
  (js/window.history.pushState nil nil path-str)
  (fsm/dispatch fsm {:type :push :url path-str}))

(defn route
  []
  (get @fsm :context))

#_(fsm/subscribe
   fsm
   (fn [{:keys [prev next action]}]
     (pprint action)))

(comment
  @router)
