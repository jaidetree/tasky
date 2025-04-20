(ns dev.jaide.tasky.features.sidebar.edit-task
  (:require
   [promesa.core :as p]
   [dev.jaide.finity.core :as fsm]
   [dev.jaide.valhalla.core :as v]
   [dev.jaide.tasky.utils :refer [class-names]]
   [dev.jaide.tasky.tasks :refer [task-validator session-validator fetch-task]]
   [dev.jaide.tasky.state-machines :refer [ratom-fsm]]))

(defn task-view
  []
  [:div "Task View"])


