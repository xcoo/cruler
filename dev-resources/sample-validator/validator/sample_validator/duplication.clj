(ns sample-validator.duplication
  (:require [clojure.string :as string]
            [cruler.core :as cruler]))

(defn- check-duplication [file-data]
  (->> file-data
       (string/split-lines)
       (map string/trim)
       (map-indexed vector)
       (reduce (fn [acc [idx value]]
                 (merge-with concat acc {value (list idx)}))
               {})
       (filter #(> (count (second %)) 1))
       (mapcat second)
       (map inc)
       sort))

(defmethod cruler/validate ::duplication
  [_ data]
  {:errors (->> data
                (map (fn [x]
                       {:file-path (:file-path x)
                        :error-keys (check-duplication (:raw-content x))}))
                (filter #(seq (:error-keys %))))
   :message "Duplicated line is found at line"})
