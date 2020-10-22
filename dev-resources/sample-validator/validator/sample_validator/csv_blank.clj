(ns sample-validator.csv-blank
  (:require [clojure.string :as string]
            [cruler.core :as cruler]))

(defn- build-error
  [{:keys [file-path parsed-content]}]
  (keep (fn [coll]
          (when-let [keys (->> (map-indexed vector coll)
                               (filter #(string/blank? (second %)))
                               (map first)
                               seq)]
            {:file-path file-path
             :error-block coll
             :error-keys keys}))
        parsed-content))

(defmethod cruler/validate ::csv-blank
  [_ data]
  {:errors (->> (filter #(= (:file-type %) :csv) data)
                (mapcat build-error))
   :message "Blank column is found at line"})
