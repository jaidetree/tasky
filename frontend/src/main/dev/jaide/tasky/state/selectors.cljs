(ns dev.jaide.tasky.state.selectors
  (:require
   [reagent.core :as r]
   [dev.jaide.tasky.router :refer [router-fsm]]
   [dev.jaide.tasky.state.tasks-fsm :refer [tasks-fsm find-task-fsm] :as tasks-fsm]
   [dev.jaide.tasky.utils :as u]))

(def selected-task-id
  (r/reaction
   (let [task-id (get-in router-fsm [:routes "tasks"])]
     (if (empty? task-id)
       nil
       task-id))))

(def all-task-fsms
  (r/reaction
   (->> (get tasks-fsm :tasks)
        (map :fsm))))

(def all-tasks
  (r/reaction (->> @all-task-fsms
                   (map :task))))

(def selected-task-fsm
  (r/reaction
   (when-let [task-id @selected-task-id]
     (->> @all-task-fsms
          (u/find #(= (get-in % [:task :id]) task-id))))))

(def all-child-task-fsms
  (r/reaction
   (let [tasks @all-task-fsms
         parent-id @selected-task-id]
     (->> tasks
          (filter #(= (get-in % [:task :parent_task_id])
                      parent-id))))))

(def all-child-tasks
  (r/reaction
   (->> @all-child-task-fsms
        (map :task))))

(defn- filter-child-task-fsms
  [task-fsms parent-id]
  (->> task-fsms
       (filter #(= (get-in % [:task :parent_task_id])
                   parent-id))))

(defn child-task-fsms
  [parent-id]
  (let [task-fsms @all-task-fsms]
    (r/track filter-child-task-fsms task-fsms parent-id)))

(defn child-tasks
  [parent-id]
  (r/reaction
   (let [child-tasks @(child-task-fsms parent-id)]
     (->> child-tasks
          (map :task)))))

(def tasks-by-id
  (r/reaction
   (let [tasks @all-tasks]
     (->> tasks
          (reduce
           (fn [m task]
             (assoc m (:id task) task))
           {})))))

(defn task-by-id
  [task-id]
  @(r/reaction
    (let [tasks @tasks-by-id]
      (get tasks task-id))))

(defn session-by-id
  [task-id session-id]
  @(r/reaction
    (when-let [task (task-by-id task-id)]
      (u/find #(= (:id %) session-id)
              (:time_sessions task)))))
