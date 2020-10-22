(ns cruler.config
  (:require [clojure.edn :as edn]))

(def base-config
  {:validators {}
   :paths ["validator"]
   :deps []})

(defn- parse-config
  [m]
  (let [config (merge base-config m)
        validators (->> (:validators config)
                        (map (fn [[k v]] [k (map re-pattern v)]))
                        (into {}))]
    (assoc config :validators validators)))

(defn load-config
  [file]
  (parse-config (edn/read-string (slurp file))))
