(ns dev.jaide.tasky.views.table
  (:require
   [reagent.core :refer [class-names]]))

(defn th
  [{:as attrs} & children]
  (into
   [:th.py-3.px-4.text-left.bg-gray-200
    (merge attrs
           {:class (class-names "bg-gray-200 bg-slate-600" (:class attrs))})]
   children))

(defn td
  [{:as attrs} & children]
  (into
   [:td.py-2.px-4.text-sm
    (or attrs {})]
   children))
