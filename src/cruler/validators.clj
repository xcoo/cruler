(ns cruler.validators
  (:require [clojure.string :as string]
            [cruler.core :as cruler]))

(defn- blank-file?
  [{:keys [raw-content]}]
  (= (string/trim raw-content) ""))

(defn- check-blank-line
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

(defmethod cruler/validate ::start-of-file
  [_ data]
  (let [files (filter #(re-find #"\A\s+" (:raw-content %)) data)]
    {:errors (map #(select-keys % [:file-path]) files)
     :message "The file must not start with whitespaces"}))

(defmethod cruler/validate ::trailing-whitespace
  [_ data]
  (let [files (filter #(re-find #"(?m)[ \t]+$" (:raw-content %)) data)]
    {:errors (map #(select-keys % [:file-path]) files)
     :message "Trailing whitespaces must be removed"}))

(defmethod cruler/validate ::end-of-file
  [_ data]
  (let [files (remove #(re-find #"(\A|[^\n]+\n)\z" (:raw-content %)) data)]
    {:errors (map #(select-keys % [:file-path]) files)
     :message "The file must end with a single newline."}))

(defmethod cruler/validate ::blank-line
  [_ data]
  (let [errors (->> data
                    (remove blank-file?)
                    (filter (fn [d] (.contains [:csv :text] (:file-type d))))
                    (mapcat check-blank-line)
                    (filter #(seq (:error-keys %))))]
    {:errors errors
     :message "Blank line is found at line"}))
