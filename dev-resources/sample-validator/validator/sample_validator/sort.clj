(ns sample-validator.sort
  (:require [clojure.string :as string]
            [cruler.core :as cruler]))

(defn- test-sort
  [compare-fn xs]
  (loop [prev-x nil
         idx 0
         xs* xs]
    (if (and prev-x (>= (compare-fn prev-x (first xs*)) 1))
      idx
      (when (seq (rest xs*))
        (recur (first xs*) (inc idx) (rest xs*))))))

(defn- check-sorted [data]
  (->> (:raw-content data)
       (string/split-lines)
       (remove #(= "" %))
       (map #(-> % string/trim string/lower-case))
       (test-sort compare)
       (vector)
       (remove nil?)
       (map inc)))

(defmethod cruler/validate ::sort
  [_ data]
  {:errors (->> data
                (map (fn [x]
                       {:file-path (:file-path x)
                        :error-keys (check-sorted x)}))
                (filter #(seq (:error-keys %))))
   :message "Sort error: not ascending order at line"})
