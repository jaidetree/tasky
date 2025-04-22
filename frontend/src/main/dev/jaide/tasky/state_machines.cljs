(ns dev.jaide.tasky.state-machines
  (:refer-clojure :exclude [atom])
  (:require
   [reagent.core :refer [atom]]
   [dev.jaide.finity.core :refer [atom-fsm subscribe]]))

(defonce prev-states (atom {}))

(defn ratom-fsm
  "Reagent wrapper for finity atom-fsm function"
  [spec & {:keys [id initial _effect] :as opts}]
  (let [id (or id (:fsm/id @spec))
        fsm (atom-fsm spec (merge {:atom atom
                                   :initial (get @prev-states id)}
                                  opts))]
    (when (not initial)
      (subscribe fsm
                 (fn [{:keys [next]}]
                   (swap! prev-states assoc id next))))
    fsm))

(comment
  @prev-states
  (reset! prev-states {}))
