(ns sample-validator.reference
  (:require [clojure.set :as cset]
            [cruler.core :as cruler]))

(defn- get-keys [m ks]
  (map #(get m %) ks))

(defn- collect-values
  [data key]
  (->> (mapcat :parsed-content data)
       (map #(get % key))
       set))

(defn- build-error
  [{:keys [file-path parsed-content]} keys defined]
  (let [missings (cset/difference
                  (set (mapcat #(get-keys % keys) parsed-content)) defined)]
    (map (fn [id]
           {:file-path file-path
            :error-block (first
                          (filter #((set (get-keys % keys)) id) parsed-content))
            :error-value id
            :error-keys keys})
         missings)))

(defmethod cruler/validate ::approval<->drug
  [_ data]
  (let [drug-data (filter #(re-find #"drug/[\w-]+\.ya?ml$" (:file-path %)) data)
        approval-data (filter #(re-find #"approved-drug/[\w-]+\.ya?ml$" (:file-path %)) data)
        defined-drugs (collect-values drug-data :id)]
    {:errors (mapcat #(build-error % [:drug] defined-drugs) approval-data)
     :message "Unknown drug ids:"}))
