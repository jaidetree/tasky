(ns dev.jaide.tasky.views.diagrams
  (:require
   [reagent.core :as r]
   [dev.jaide.finity.core :as fsm]
   [dev.jaide.tasky.state.task-fsm :refer [task-fsm-spec new-task-fsm-spec]]
   [dev.jaide.tasky.state.tasks-fsm :refer [tasks-fsm-spec]]
   [dev.jaide.tasky.views.delete-rocker :refer [delete-rocker-fsm-spec]]
   [dev.jaide.tasky.router :refer [router-fsm-spec]]))

(def specs
  [router-fsm-spec
   tasks-fsm-spec
   task-fsm-spec
   new-task-fsm-spec
   delete-rocker-fsm-spec])

(defn run-mermaid!
  []
  (when js/window.mermaid
    (js/window.mermaid.run #js {:querySelector ".mermaid"})))

(defn diagrams
  []
  [:div.flex.flex-col.justify-center.items-center.gap-8
   {:ref run-mermaid!}
   (for [spec specs]
     [:pre
      {:key (:fsm/id @spec)
       :class "mermaid w-full flex flex-row justify-center"}
      (str
       "---\ntitle: " (:fsm/id @spec) "\n---\n"
       (fsm/spec->diagram spec))])])

