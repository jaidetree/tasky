(ns dev.jaide.tasky.views.delete-rocker
  (:require
   [reagent.core :refer [class-names with-let]]
   [dev.jaide.finity.core :as fsm]
   [dev.jaide.finity.core :as fsm]
   [dev.jaide.tasky.events :refer [on]]
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
  [{:keys [dispatch fsm state effect]}]
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
  [{:keys [dispatch fsm state]}]
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
                         :percent (v/number)}}

     :actions {:mouse-down {:x (v/number) :event (v/instance js/MouseEvent)}
               :mouse-move {:x (v/number) :percent (v/number)}
               :mouse-up {}
               :cancel {}}

     :effects  {:start-drag [{:thumb (v/instance js/HTMLButtonElement)
                              :track (v/instance js/HTMLDivElement)}
                             (fn [{:keys [] :as opts}]
                               (let [dispose-mouse-move (on js/window :mousemove (mouse-move-handler opts))
                                     dispose-mouse-up (on js/window :mouseup (mouse-up-handler opts))
                                     dispose-key-up (on js/window :keyup (key-up-handler opts))]
                                 (fn []
                                   (dispose-mouse-move)
                                   (dispose-mouse-up)
                                   (dispose-key-up))))]}
     :transitions
     [{:from [:ready]
       :actions [:mouse-down]
       :to [:dragging]
       :do (fn [state action]
             {:state :dragging
              :context {:x 0 :percent 0}
              :effect (let [thumb (-> action :event .-currentTarget)
                            track (-> thumb .-parentElement)]
                        (-> action :event .preventDefault)
                        {:id :start-drag
                         :thumb thumb
                         :track track})})}

      {:from [:dragging]
       :actions [:mouse-move]
       :to [:dragging]
       :do (fn [state action]
             {:state :dragging
              :context (merge
                        (get state :context)
                        {:x (:x action)
                         :percent (:percent action)})
              :effect (:effect state)})}

      {:from [:dragging]
       :actions [:cancel]
       :to :ready}

      {:from [:dragging]
       :actions [:mouse-up]
       :to [:deleting :ready]
       :do (fn [state _action]
             (if (>= (get-in state [:context :percent]) 100)
               {:state :deleting
                :context (:context state)}
               {:state :ready}))}]}))

(defn create
  [id]
  (doto (ratom-fsm delete-rocker-fsm-spec
                   {:id (str "delete-rocker-" id)
                    :initial {:state :ready
                              :context {}}})
    (fsm/subscribe
     (fn [{:keys [next action]}]
       #_(cljs.pprint/pprint action)))))

(defn color-mix
  [percent color-a color-b]
  (str "color-mix(in srgb, " color-a ", " color-b " " (.toFixed percent 2) "%)"))

(defn track
  [{:keys [fsm]} & children]
  (let [{:keys [state context]} @fsm]
    (into [:div
           {:class (class-names
                    "rounded-full w-16 p-1"
                    (when (not= state :tracking)
                      "bg-zinc-700"))
            :style {:background (when (= state :dragging)
                                  (color-mix (get context :percent)
                                             "var(--color-zinc-700)"
                                             "var(--color-red-900)"))}}]
          children)))

(defn thumb
  [{:keys [fsm]}]
  (let [{:keys [state context]} @fsm
        x (get context :x 0)
        percent (get context :percent)]
    [:button
     {:type "button"
      :class (class-names "block rounded-full size-4"
                          (when (not= state :dragging)
                            "bg-slate-500"))
      :style {:transform (str "translateX(" x "px)")
              :background (when (= state :dragging)
                            (color-mix percent "var(--color-slate-500)" "var(--color-red-500)"))}
      :on-mousedown #(fsm/dispatch fsm {:type :mouse-down :event % :x 0})}
     [:span.sr-only "Drag to the right to delete"]]))

(defn create-fsm
  [{:keys [id on-delete]}]
  (let [fsm (ratom-fsm delete-rocker-fsm-spec
                       {:id (str "delete-rocker-" id)
                        :initial {:state :ready
                                  :context {}}})
        dispose (fsm/subscribe
                 fsm
                 (fn [{:keys [prev next]}]
                   (when (and (= (:state next) :deleting)
                              (not= (:state prev) (:state next)))
                     (on-delete))))]
    [fsm (fn []
           (dispose)
           (fsm/destroy fsm))]))

(defn delete-rocker
  [{:keys [on-delete]}]
  (with-let [[fsm dispose] (create-fsm {:on-delete on-delete})]
    [track
     {:fsm fsm}
     [thumb
      {:fsm fsm}]]
    (finally
      (dispose))))

