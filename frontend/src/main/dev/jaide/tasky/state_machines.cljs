(ns dev.jaide.tasky.state-machines
  (:refer-clojure :exclude [atom])
  (:require
   [reagent.core :refer [atom]]
   [dev.jaide.finity.core :refer [atom-fsm subscribe]]
   [dev.jaide.tasky.features.toaster :refer [toast]]))

(defn ratom-fsm
  "Reagent wrapper for finity atom-fsm function"
  [spec & {:keys [_id _initial _effect] :as opts}]
  (let [opts (merge {:atom atom} opts)]
    (doto (atom-fsm spec opts)
      (subscribe
       (fn [{:keys [action]}]
         (when (= (:type action) :error)
           (toast {:type :error
                   :title "Error"
                   :content (.toString (:error action))
                   :duration nil})))))))


