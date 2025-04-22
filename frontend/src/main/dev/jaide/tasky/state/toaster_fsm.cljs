(ns dev.jaide.tasky.state.toaster-fsm
  (:require
   [reagent.core :refer [atom]]
   [dev.jaide.finity.core :as fsm]
   [dev.jaide.valhalla.core :as v]))

(def any (v/assert (constantly true)))

(def toast-validator
  (v/record
   {:id (v/string)
    :title (v/string)
    :message any
    :type (v/enum [:error :success :info])
    :duration (v/number)}))

(def toasts-validator
  (v/vector toast-validator))

(def toaster-fsm-spec
  (fsm/define
    {:id :toaster

     :initial {:state :ready
               :context {}}

     :states {:ready {}
              :popped {:toasts toasts-validator}}

     :actions {:pop {:toast toast-validator}
               :remove {:id (v/string)}}

     :transitions
     [{:from [:ready]
       :actions [:pop]
       :to [:popped]
       :do (fn [state action]
             {:state :popped
              :context {:toasts [(:toast action)]}})}

      {:from [:popped]
       :actions [:pop]
       :to [:popped]
       :do (fn [state action]
             {:state :popped
              :context {:toasts (cons (:toast action) (get-in state [:context :toasts]))}})}

      {:from [:popped]
       :actions [:remove]
       :to [:popped :ready]
       :do (fn [state action]
             (let [id (:id action)
                   toasts (->> (get-in state [:context :toasts])
                               (remove #(= (:id %) id))
                               (vec))]
               (if (empty? toasts)
                 {:state :ready}
                 {:state :popped
                  :context {:toasts toasts}})))}]}))

;; Normally FSMs should be defined with the ratom-fsm func but in this case,
;; there shouldn't be any error subscriptions created as they are sent here
(def toaster-fsm (fsm/atom-fsm toaster-fsm-spec {:atom atom}))
