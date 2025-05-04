(ns dev.jaide.tasky.views.transition
  (:require
   [clojure.string :as s]
   [cljs.core :refer [IDeref]]
   [promesa.core :as p]
   [reagent.core :as r]
   [dev.jaide.finity.core :as fsm]
   [dev.jaide.valhalla.core :as v]
   [dev.jaide.tasky.dom :refer [abort-controller on promise-with-resolvers]]))

(defn request-animation-frame
  [signal]
  (p/race
   [(p/create
     (fn [resolve _reject]
       (js/window.requestAnimationFrame resolve)))
    (p/create
     (fn [_resolve reject]
       (when signal
         (if (.-aborted signal)
           (reject (.-reason signal))
           (.addEventListener signal "abort" #(reject (.-reason signal)))))))]))

(defn- timeout
  [delay]
  (p/create
   (fn [resolve]
     (js/setTimeout #(resolve) delay))))

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
  (let [state (r/atom {:phase :idle :progress 0})
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
  "Control the progression of a CSS transition"
  (start
    [this]
    "Start the transition returns a promise representing when the transition
    completes")
  (end
    [this]
    "Signify the transition has completed, used to remove transition classes.
    Should be called on transitionend events")
  (abort
    [this]
    "Cancel the transition"))

(def default-state
  {:enter false
   :from false
   :to false})

(deftype AtomTransition [state-ref abort-ref]
  ITransition
  (abort [this]
    (when-let [abort @abort-ref]
      (abort this))
    (reset! state-ref default-state)
    (reset! abort-ref nil))

  (start [this]
    (abort this)
    (let [[signal abort] (abort-controller)]
      (reset! state-ref (assoc default-state :enter true))
      (reset! abort-ref abort)
      (-> (p/do
            (request-animation-frame signal)
            (swap! state-ref assoc :from true)
            (request-animation-frame signal)
            (swap! state-ref assoc :to true :from false))
          (p/catch
           (fn [_error]
             (abort this)
             nil)))))

  (end [this]
    (let [[signal abort] (abort-controller)]
      (-> (p/do
            (request-animation-frame signal)
            #_(swap! state-ref assoc :enter false))
          (p/catch
           (fn [_error]
             (abort this)
             nil)))))

  IDeref
  (-deref [_this]
    @state-ref))

(defn create
  [& [initial]]
  (new AtomTransition
       (r/atom (or initial default-state))
       (r/atom nil)))

(def subscriber-validator (v/assert fn?))

(defn- broadcast-to-subs
  [event subscribers]
  (doseq [subscriber subscribers]
    (subscriber event)))

(def subscriptions-fsm-spec
  (fsm/define
    {:id :transition-subscriptions-fsm

     :initial {:state :inactive
               :context {}}

     :states {:active {:subscribers (v/set subscriber-validator)}
              :inactive {}}

     :actions {:subscribe {:handler subscriber-validator}
               :unsubscribe {:handler subscriber-validator}}

     :effects {:broadcast [{}
                           (fn [{:keys [fsm]}]
                             (on js/document.body "transitionend"
                                 #(broadcast-to-subs % (get fsm :subscribers))))]}

     :transitions
     [{:from [:inactive]
       :actions [:subscribe]
       :to [:active]
       :do (fn [_state action]
             {:state :active
              :context {:subscribers #{(:handler action)}}
              :effect :broadcast})}

      {:from [:active]
       :actions [:subscribe]
       :to [:active]
       :do (fn [state action]
             (update-in state [:context :subscribers] conj (:handler action)))}

      {:from [:active]
       :actions [:unsubscribe]
       :to [:active :inactive]
       :do (fn [state action]
             (let [subs (->> (get-in state [:context :subscribers])
                             (remove #(= % (:handler action)))
                             (set))]
               (if (empty? subs)
                 {:state :inactive
                  :context {}}
                 (assoc-in state [:context :subscribers] subs))))}]}))

(def ^:private fsm (fsm/atom-fsm subscriptions-fsm-spec {:atom r/atom}))

(defn- end-transition
  [event tr props]
  (let [class-name (.. event -target -className)]
    (when (and (:active props)
               (s/includes? class-name (:enter props))
               (s/includes? class-name (:to props)))
      (end tr))))

(defn- track-class
  [{:keys [active enter from to] :as props}]
  (r/with-let [default-state (merge default-state (when active
                                                    {:from true}))
               tr (create default-state)
               handler #(end-transition % tr props)
               _ (fsm/dispatch fsm {:type :subscribe :handler handler})
               ref #js {:current false}]

    (let [state @tr]
      (when (and active (false? (.-current ref)))
        (set! (.-current ref)
              true)
        (p/do
          (request-animation-frame nil)
          (start tr)))
      (r/class-names
       (when (:enter state) enter)
       (when (:from state) from)
       (when (:to state) to)))
    (finally
      (abort tr)
      (fsm/dispatch fsm {:type :unsubscribe :handler handler}))))

(defn class
  "Assign class names to a transition phase such as :enter :from and :to

  It also handles ending the transition by using a global transitionend listener

  Arguments:
  - opts - A hash-map of options

  Options:
  - :active - Toggle the transition on or off
  - :enter - String of class names to apply when transitioning
  - :from - The class names when the transition starts
  - :to - The class names to switch to. :from class is replaced with :to

  Returns a string of class names
  "
  [{:keys [active enter from to]}]
  @(r/track track-class {:active active
                         :enter enter
                         :from from
                         :to to}))

