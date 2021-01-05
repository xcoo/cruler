(ns cruler.report
  (:require [cruler.log :as log]))

; TODO I want to private "report-counter"
(def report-counter
  (atom {:validate 0, :pass 0, :fail 0}))

(defmulti report :type)

(defmethod report :default [m]
  (swap! report-counter #(update % :validate inc))
  (prn m))

(defmethod report :pass [_]
  (swap! report-counter #(-> %
                             (update :validate inc)
                             (update :pass inc))))

(defmethod report :fail [m]
  (swap! report-counter #(-> %
                             (update :validate inc)
                             (update :fail inc)))
  (log/error "\nERROR at" (:validator m) "validator")
  (binding [log/*colorize?* false]
    (log/error (:message m))))

(defmethod report :summary [m]
  (println "\nRan" (:validate m) "validations.")
  (println (str (:pass m) " passes, " (:fail m) " failures.")))
