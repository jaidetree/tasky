(ns dev.jaide.tasky.dom)

(defn on
  [target event-name handler & {:as opts}]
  (let [event-str (clj->js event-name)]
    (.addEventListener target event-str handler (clj->js (or opts {})))
    (fn off [_]
      (.removeEventListener target event-str handler))))

(defn timeout
  [ms f]
  (let [timer (js/setTimeout f ms)]
    (fn dispose
      []
      (js/clearTimeout timer))))

(defn interval
  [ms f]
  (let [timer (js/setInterval f ms)]
    (fn dispose
      []
      (js/clearInterval timer))))

(defn abort-controller
  []
  (let [ac (new js/AbortController)]
    [(.-signal ac)
     #(.abort ac)]))

(defn promise-with-resolvers
  []
  (let [p-obj (js/Promise.withResolvers)]
    [(.-promise p-obj)
     (.-resolve p-obj)
     (.-reject p-obj)]))

