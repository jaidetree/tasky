(ns dev.jaide.tasky.features.toaster
  (:require
   [reagent.core :refer [atom class-names with-let]]
   [dev.jaide.finity.core :as fsm]
   [dev.jaide.valhalla.core :as v]
   [dev.jaide.tasky.dom :refer [timeout]]
   [dev.jaide.tasky.views.transition :as tr]
   ["@heroicons/react/24/outline" :refer [XMarkIcon]]))

(def any (v/assert (constantly true)))

(def toast-validator
  (v/record
   {:id (v/string)
    :title (v/string)
    :content any
    :type (v/enum [:error :success :info])
    :duration (v/nilable (v/number))
    :dismissable (v/boolean)}))

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
               :dismiss {:id (v/string)}}

     :effects {}

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
       :actions [:dismiss]
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

(defn toast
  [{:keys [content type duration title dismissable]
    :or {duration 3000
         type :info
         dismissable true}}]
  (fsm/dispatch toaster-fsm
                {:type :pop
                 :toast {:type type
                         :id (str (random-uuid))
                         :title title
                         :content content
                         :duration duration
                         :dismissable dismissable}}))

(defn dismiss
  [id]
  (fsm/dispatch toaster-fsm {:type :dismiss :id id}))

(defn toast-message
  [{:keys [toast]}]
  (let [{:keys [id type title content duration dismissable]} toast]
    (with-let [clear-timeout (when duration
                               (timeout duration #(dismiss id)))
               state-ref (atom nil)]
      [:div
       {:class
        (-> (class-names
             "border-l-4 bg-stone-950 p-4 rounded-md w-96 shadow-lg text-base/5 relative"
             (if (= @state-ref :dismissing)
               (tr/class
                {:active true
                 :enter "transition-all duration-250 ease-in"
                 :from "translate-y-0 opacity-100"
                 :to "translate-y-[120%] opacity-0"})
               (tr/class
                {:active true
                 :enter "transition-transform duration-500 ease-out"
                 :from "translate-x-[120%]"
                 :to "translate-x-0"}))
             (case type
               :error "border-l-red-500"
               :success "border-l-green-500"
               :info "border-l-blue-500"))
            (doto println))
        :on-transition-end (fn [_event]
                             (when (= @state-ref :dismissing)
                               (dismiss id)))}
       (when dismissable
         [:button
          {:class "rounded-full bg-black border border-white p-1 absolute -right-2 -top-2"
           :on-click #(reset! state-ref :dismissing)}
          [:> XMarkIcon
           {:class "size-3 text-white"}]])
       [:h3
        {:class "font-bold mb-2"}
        title]
       [:div.text-sm content]]
      (finally
        (when (fn? clear-timeout)
          (clear-timeout))))))

(defn toaster
  []
  [:div
   {:class "fixed right-4 top-4 flex flex-col gap-4"}
   (let [current @toaster-fsm
         state (:state current)
         toasts (get-in current [:context :toasts] [])]
     (for [toast toasts]
       [toast-message
        {:toast toast}]))])

(fsm/subscribe toaster-fsm
               (fn [{:keys [next action]}]
                 #_(cljs.pprint/pprint {:action action
                                        :state next})))

#_(timeout 100
           #(toast
             {:type :success
              :duration nil
              :title "Test Toast Message"
              :content "This is a test toast message to confirm this system is functioning correctly."}))
