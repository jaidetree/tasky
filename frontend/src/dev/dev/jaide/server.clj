(ns dev.jaide.server
  (:require
   [clojure.string :as s]
   [clojure.pprint :refer [pprint]]))

(defn proxy-predicate
  [ex _config]
  (let [uri (.getRequestURI ex)]
    (s/starts-with? uri "/api")))

