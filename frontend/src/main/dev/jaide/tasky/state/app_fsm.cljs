(ns dev.jaide.tasky.state.app-fsm
  (:require
   [dev.jaide.finity.core :as fsm]
   [dev.jaide.valhalla.core :as v]
   [dev.jaide.tasky.state-machines :refer [ratom-fsm]]))

(def app-fsm-spec
  (fsm/define
    {:id :app

     :initial {:state :closed
               :context {}}

     :states {:closed {}
              :view-task {}
              :new-task {}}

     :actions {:open-task {}
               :new-task {}
               :close {}}

     :effects {:test {:args {:blah (v/string)}
                      :do (fn [{:keys [dispatch]}]
                            nil)}}

     :transitions
     [{:from [:closed :view-task]
       :actions [:new-task]
       :to :new-task}

      {:from [:new-task]
       :actions [:open-task]
       :to :view-task}]}))

(def app-fsm (ratom-fsm app-fsm-spec))

(comment
  (cljs.pprint/pprint @app-fsm))

