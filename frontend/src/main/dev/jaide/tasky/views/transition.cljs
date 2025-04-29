(ns dev.jaide.tasky.views.transition
  (:require
   [cljs.core :refer [IDeref]]
   [promesa.core :as p]
   [reagent.core :refer [atom] :rename {atom ratom}]
   [dev.jaide.valhalla.core :as v]
   [dev.jaide.finity.core :as fsm]
   [dev.jaide.tasky.dom :refer [abort-controller promise-with-resolvers]]))

(def context-duration {:duration (v/number)})

(defn request-animation-frame
  [signal]
  (p/race
   [(p/create
     (fn [resolve _reject]
       (js/window.requestAnimationFrame resolve)))
    (p/create
     (fn [_resolve reject]
       (if (.-aborted signal)
         (reject (.-reason signal))
         (.addEventListener signal "abort" #(reject (.-reason signal))))))]))

(defn- frame-loop
  [duration signal callback]
  (p/let [started (request-animation-frame signal)
          [promise resolve reject] (promise-with-resolvers)
          frame-loop* (fn [f]
                        (-> (p/let [ts (request-animation-frame signal)]
                              (let [progress (/ (- ts started)
                                                duration)]
                                (callback progress)
                                (if (>= progress 1)
                                  (resolve)
                                  (f f))))
                            (p/catch
                             (fn [_reason]
                               (reject)))))]
    (frame-loop* frame-loop*)
    promise))

(defn animate
  "Loops requestAnimationFrame until duration is reached

  Arguments:
  - opts - Hash-map of keyword options

  Options:
  - :duration - Number representing the number of ms to run the animation for

  Returns a tuple vector with [state-atom, abort-fn, promise]"
  [{:keys [duration]}]
  (let [state (ratom {:phase :idle :progress 0})
        [signal abort] (abort-controller)]
    [state
     abort
     (-> (p/do
           (request-animation-frame signal)
           (reset! state {:phase :enter :progress 0})
           (frame-loop duration signal
                       #(reset! state {:phase :animate :progress %}))
           (request-animation-frame signal)
           (reset! state {:phase :complete :progress 100}))
         (p/catch (fn [_error]
                    nil)))]))

(defprotocol ITransition
  "Defines a set of functions for manipulating a transition implementation.
  Unlike animation, a transition should be controlled externally for example
  call `(end transition)` on transitionend events"
  (start
    [this]
    "Start the transition returns a promise representing when the transition
    completes")
  (end
    [this]
    "Complete the transition. Best invoked on a transitionend event")
  (abort
    [this]
    "Cancel the transition"))

(deftype AtomTransition [phase-ref abort-ref]
  ITransition
  (abort [this]
    (when-let [abort @abort-ref]
      (abort this))
    (reset! phase-ref :idle)
    (reset! abort-ref nil))

  (start [this]
    (abort this)
    (let [[signal abort] (abort-controller)]
      (reset! phase-ref :idle)
      (reset! abort-ref abort)
      (-> (p/do
            (request-animation-frame signal)
            (reset! phase-ref :enter)
            (request-animation-frame signal)
            (reset! phase-ref :transition))
          (p/catch
           (fn [_error]
             (abort this)
             nil)))))

  (end [this]
    (let [[signal abort] (abort-controller)]
      (reset! abort-ref abort)
      (-> (p/do
            (request-animation-frame signal)
            (reset! phase-ref :complete))
          (p/catch
           (fn [_error]
             (reset! phase-ref :idle)
             (reset! abort-ref nil)
             nil)))))

  IDeref
  (-deref [this]
    @phase-ref))

(defn create
  []
  (new AtomTransition (ratom :idle) (ratom nil)))
