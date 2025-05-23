(ns dev.jaide.tasky.views.delete-rocker
  (:require
   [reagent.core :refer [class-names with-let]]
   [dev.jaide.finity.core :as fsm]
   [dev.jaide.tasky.dom :refer [on timeout]]
   [dev.jaide.valhalla.core :as v]
   [dev.jaide.valhalla.js :as vjs]
   [dev.jaide.tasky.state-machines :refer [ratom-fsm]]))

(def rect-parser
  (vjs/record {:left (v/number)
               :right (v/number)
               :top (v/number)
               :bottom (v/number)
               :width (v/number)
               :height (v/number)
               :x (v/number)
               :y (v/number)}))

(defn el->rect
  [element]
  (let [rect (.getBoundingClientRect element)]
    (v/parse rect-parser rect)))

(comment
  (-> (js/document.querySelector "button")
      (el->rect)))

(defn mouse-move-handler
  [{:keys [dispatch]} effect]
  (fn [event]
    (let [{:keys [thumb track]} effect
          thumb-rect (el->rect thumb)
          track-rect (el->rect track)
          page-x (-> event .-pageX)
          thumb-offset (:width thumb-rect)
          padding-left (-> track (js/window.getComputedStyle) .-paddingLeft (js/parseFloat 10) (* 2))
          track-width (.-offsetWidth track)
          track-limit (- track-width thumb-offset padding-left)
          x (-> (- page-x
                   (:left track-rect))
                (- padding-left)
                (- (/ thumb-offset 2))
                (js/Math.max 0)
                (js/Math.min track-limit))
          percent (-> (/ x track-limit)
                      (* 100)
                      (js/Math.min 100)
                      (js/Math.max 0))]
      (dispatch {:type :mouse-move
                 :x x
                 :percent percent}))))

(defn mouse-up-handler
  [{:keys [dispatch]}]
  (fn [event]
    (dispatch {:type :mouse-up})))

(defn key-up-handler
  [{:keys [dispatch]}]
  (fn [event]
    (case (.-key event)
      "Escape" (dispatch {:type :cancel})
      nil)))

(def delete-rocker-fsm-spec
  (fsm/define
    {:id :delete-rocker

     :initial {:state :ready
               :context {}}

     :states {:ready {}
              :dragging {:x (v/number)
                         :percent (v/number)}
              :deleting {:x (v/number)
                         :percent (v/number)}
              :holding {:x (v/number)
                        :percent (v/number)}}

     :actions {:mouse-down {:x (v/number) :event (v/instance js/MouseEvent)}
               :mouse-move {:x (v/number) :percent (v/number)}
               :mouse-up {}
               :cancel {}
               :delete {}}

     :effects  {:start-drag {:args {:thumb (v/instance js/HTMLButtonElement)
                                    :track (v/instance js/HTMLDivElement)}
                             :do (fn [{:keys [] :as opts} effect]
                                   (let [dispose-mouse-move (on js/window :mousemove (mouse-move-handler opts effect))
                                         dispose-mouse-up (on js/window :mouseup (mouse-up-handler opts))
                                         dispose-key-up (on js/window :keyup (key-up-handler opts))]
                                     (fn []
                                       (dispose-mouse-move)
                                       (dispose-mouse-up)
                                       (dispose-key-up))))}}

     :transitions
     [{:from [:ready]
       :actions [:mouse-down]
       :to [:dragging]
       :do (fn [state action]
             {:state :dragging
              :context {:x 0 :percent 0}
              :effects {:start-drag (let [thumb (-> action :event .-currentTarget)
                                          track (-> thumb .-parentElement)]
                                      (-> action :event .preventDefault)
                                      {:thumb thumb
                                       :track track})}})}

      {:from [:dragging :holding]
       :actions [:mouse-move]
       :to [:dragging :holding]
       :do (fn [state action]
             (let [context (merge
                            (:context state)
                            {:x (:x action)
                             :percent (:percent action)})
                   effects (get state :effects)]
               (if (= (:percent action) 100)
                 {:state :holding
                  :context context
                  :effects effects}
                 {:state :dragging
                  :context context
                  :effects effects})))}

      {:from [:dragging :holding]
       :actions [:cancel :mouse-up]
       :to :ready}

      {:from [:holding]
       :actions [:delete]
       :to [:deleting]
       :do (fn [state]
             {:state :deleting
              :context (:context state)})}]}))

(comment
  (fsm/spec->diagram delete-rocker-fsm-spec))

(def countdown-fsm-spec
  (fsm/define
    {:id :countdown-timer

     :initial {:state :inactive
               :context {}}

     :states {:inactive {}
              :active {:remaining (v/number)}
              :complete {}}

     :actions {:start {:seconds (v/number)}
               :tick {}}

     :effects {:set-timer
               {:args {:remaining (v/number)}
                :do (fn [{:keys [dispatch]}]
                      (timeout 1000
                               #(dispatch {:type :tick})))}}

     :transitions
     [{:from [:inactive]
       :actions [:start]
       :to [:active]
       :do (fn [_state {:keys [seconds]}]
             {:state :active
              :context {:remaining seconds}
              :effects {:set-timer {:remaining seconds}}})}

      {:from [:active]
       :actions [:tick]
       :to [:complete :active]
       :do (fn [{:keys [state context]} _action]
             (let [remaining (dec (:remaining context))]
               (if (zero? remaining)
                 {:state :complete}
                 {:state state
                  :context {:remaining remaining}
                  :effects {:set-timer {:remaining remaining}}})))}]}))

(defn color-mix
  [percent color-a color-b]
  (str "color-mix(in srgb, " color-a ", " color-b " " (.toFixed percent 2) "%)"))

(defn dragging?
  [state]
  (contains? #{:dragging :holding :deleting} state))

(defn holding?
  [state]
  (contains? #{:holding :deleting} state))

(defn hold-progress
  [{:keys [fsm]}]
  (with-let [duration 1
             timer-fsm (ratom-fsm countdown-fsm-spec
                                  {:initial {:state :inactive}})
             unsubscribe (fsm/subscribe
                          fsm
                          (fn [{:keys [next]}]
                            (when (= (:state next) :holding)
                              (fsm/dispatch timer-fsm {:type :start :seconds duration}))))]
    (let [state (:state @fsm)]
      [:div.absolute
       {:class "absolute left-0 right-0 bottom-0 top-0"}
       [:div
        {:class (class-names
                 "absolute left-0 bottom-0 top-0 w-0 transition-[width] bg-red-900"
                 (when (holding? state)
                   "w-full"))
         :on-transitionend (when (holding? state)
                             #(do
                                (.stopPropagation %)
                                (fsm/dispatch fsm {:type :delete})))
         :style {:transition-duration (if (holding? state)
                                        (str duration "s")
                                        "200ms")}}]])
    (finally
      (unsubscribe)
      (fsm/destroy timer-fsm))))

(defn track
  [{:keys [fsm]} & children]
  (let [{:keys [state context]} @fsm]
    (into
     [:div.relative.overflow-hidden
      {:class (class-names
               "rounded-full w-16 p-1"
               (when (not (dragging? state))
                 "bg-zinc-700"))
       :style {:background (when (dragging? state)
                             (color-mix (get context :percent)
                                        "var(--color-zinc-700)"
                                        "var(--color-red-800)"))}}]
     (conj children
           [hold-progress {:fsm fsm}]))))

(defn thumb
  [{:keys [fsm]}]
  (let [{:keys [state context]} @fsm
        x (get context :x 0)
        percent (get context :percent)]
    [:button
     {:type "button"
      :class (class-names "block rounded-full size-4"
                          (when (not (dragging? state))
                            "bg-slate-500"))
      :style {:transform (str "translateX(" x "px)")
              :background (when (dragging? state)
                            (color-mix percent "var(--color-slate-500)" "var(--color-red-500)"))}
      :on-mousedown #(fsm/dispatch fsm {:type :mouse-down :event % :x 0})}
     [:span.sr-only "Drag to the right to delete"]]))

(defn create-rocker-fsm
  [{:keys [id on-delete]}]
  (let [fsm (ratom-fsm delete-rocker-fsm-spec
                       {:id (str "delete-rocker-" id)
                        :initial {:state :ready
                                  :context {}}})
        unsubscribe (fsm/subscribe
                     fsm
                     (fn [{:keys [prev next action]}]
                       (when (and (= (:state next) :deleting)
                                  (not= (:state prev) (:state next)))
                         (on-delete))))]
    [fsm (fn []
           (unsubscribe)
           (fsm/destroy fsm))]))

(defn delete-rocker
  [{:keys [on-delete]}]
  (with-let [[fsm dispose] (create-rocker-fsm {:on-delete on-delete})]
    [track
     {:fsm fsm}
     [thumb
      {:fsm fsm}]]
    (finally
      (dispose))))

