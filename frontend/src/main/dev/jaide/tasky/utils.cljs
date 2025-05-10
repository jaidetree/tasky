(ns dev.jaide.tasky.utils
  (:refer-clojure :exclude [find])
  (:require
   [clojure.string :as s]))

(defn class-names
  [& class-names]
  (->> class-names
       (filter #(and (some? %) (string? %) (not (s/blank? %))))
       (s/join " ")))

(defn map->query-string
  [query-params]
  (str
   "?"
   (-> query-params
       (clj->js)
       (js/URLSearchParams.)
       (.toString))))

(defn find
  [pred? coll]
  (->> coll
       (some #(when (pred? %) %))))
