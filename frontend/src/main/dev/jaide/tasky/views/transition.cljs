(ns dev.jaide.tasky.views.transition
  (:require
   [promesa.core :as p]
   [dev.jaide.valhalla.core :as v]
   [dev.jaide.finity.core :as fsm]
   [dev.jaide.tasky.state-machines :refer [ratom-fsm]]))

(def context-duration {:duration (v/number)})

(defn request-animation-frame
  []
  (p/create
   (fn [resolve _reject]
     (js/window.requestAnimationFrame resolve))))

(defn promise-with-resolvers
  []
  (let [p-obj (js/Promise.withResolvers)]
    [(.-promise p-obj)
     (.-resolve p-obj)
     (.-reject p-obj)]))

(defn animate
  [duration]
  (p/let [started (request-animation-frame)
          [promise resolve] (promise-with-resolvers)
          frame-loop (fn [f]
                       (p/let [ts (request-animation-frame)]
                         (if (<= duration (- ts started))
                           (resolve)
                           (f f))))]
    (frame-loop frame-loop)
    promise))

(def transition-fsm-spec
  (fsm/define
    {:id :transition

     :initial {:state :idle
               :context {}}

     :states {:idle    {}
              :enter   {}
              :animate {}
              :exit    {}}

     :actions {:start    {:duration (v/number)}
               :run      {}
               :end      {}
               :complete {}
               :stop     {}}

     :effects {:animate [{}
                         (fn [{:keys [dispatch action]}]
                           (p/do
                             (request-animation-frame)
                             (dispatch :run)
                             (animate (:duration action))
                             (dispatch :end)
                             (request-animation-frame)
                             (dispatch :complete)))]}

     :transitions
     [{:from [:idle]
       :actions [:start]
       :to [:enter]
       :do (fn [_state _action]
             {:state :enter
              :context {}
              :effect :animate})}

      {:from [:enter]
       :actions [:run]
       :to [:animate]
       :do (fn [{:keys [effect]} _action]
             {:state :animate
              :context {}
              :effect effect})}

      {:from [:animate]
       :actions [:end]
       :to [:exit]
       :do (fn [{:keys [effect]} _action]
             {:state :exit
              :context {}
              :effect effect})}

      {:from [:exit]
       :actions [:complete]
       :to :idle}]}))

(defn create-transition-fsm
  [{:keys [on-complete]}]
  (doto (ratom-fsm transition-fsm-spec)
    (fsm/subscribe
     (fn [{:keys [action]}]
       (when (= (:type action) :complete)
         (on-complete))))))

