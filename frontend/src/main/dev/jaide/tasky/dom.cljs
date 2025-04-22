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


