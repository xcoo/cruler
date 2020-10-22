(ns sample-validator.spec
  (:require [clojure.spec.alpha :as s]
            [cruler.core :as cruler]
            [cruler.spec-parser :refer [defmsg]]))

(defn- build-error
  [{:keys [file-path parsed-content]} key]
  (when-not (s/valid? key parsed-content)
    (let [{:keys [::s/problems ::s/value]} (s/explain-data key parsed-content)]
      (map (fn [prob]
             {:file-path file-path
              :error-block (get-in value (drop-last (:in prob)))
              :error-value prob
              :error-keys [(last (:in prob))]})
           problems))))

(s/def :validator.spec.approval/drug string?)
(s/def :validator.spec.approval/comment (s/and string?
                                               #(re-matches #"A\d+" %)))
(s/def :validator.spec.approval/serial integer?)

(defmsg ::group "Should be A, B or C")
(s/def ::group #{"A" "B" "C"})
(s/def :validator.spec.approval/category ::group)

(s/def ::type
  (s/keys :req-un [:validator.spec.approval/serial]
          :opt-un [:validator.spec.approval/category]))
(s/def :validator.spec.approval/types (s/coll-of ::type))
(s/def ::approval
  (s/keys :req-un [:validator.spec.approval/drug
                   :validator.spec.approval/types
                   :validator.spec.approval/comment]))
(s/def ::approvals (s/and (s/coll-of ::approval)
                          #(apply distinct? (map :drug %))))

(defn- validate-with [data key]
  (let [errors (mapcat #(build-error % key) data)]
    {:errors errors}))

(defmethod cruler/validate ::approval
  [_ data]
  (validate-with data ::approvals))
