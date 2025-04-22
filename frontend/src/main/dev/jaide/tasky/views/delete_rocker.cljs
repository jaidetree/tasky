(ns dev.jaide.tasky.views.delete-rocker
  (:require
   [reagent.core :refer [class-names with-let]]
   [dev.jaide.finity.core :as fsm]
   [dev.jaide.tasky.state.delete-rocker-fsm :as dr-fsm]))

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
  (let [fsm (dr-fsm/create id)
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

