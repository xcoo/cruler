(ns cruler.config
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

(def base-config
  {:validators {}
   :paths ["validator"]
   :deps []
   :colorize true})

(def colorize
  (atom true))

(defn- parse-config
  [m]
  (let [config (merge base-config m)
        validators (->> (:validators config)
                        (map (fn [[k v]] [k (map re-pattern v)]))
                        (into {}))]
    (assoc config :validators validators)))

(defn load-config
  [dir path]
  (let [file (io/file path)
        file (if (.isAbsolute file)
               file
               (io/file dir file))]
    [(.getPath file) (parse-config (edn/read-string (slurp file)))]))
