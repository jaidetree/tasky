(ns dev.jaide.tasky.state-machines
  (:refer-clojure :exclude [atom])
  (:require
   [reagent.core :refer [atom]]
   [dev.jaide.finity.core :refer [atom-fsm]]))

(defn ratom-fsm
  "Reagent wrapper for finity atom-fsm function"
  [spec & {:keys [_state _effect] :as opts}]
  (atom-fsm spec (merge {:atom atom} opts)))


