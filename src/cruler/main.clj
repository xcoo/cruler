(ns cruler.main
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.tools.cli :as cli]
            [cruler.core :as core]
            [cruler.log :as log]
            [cruler.report :as report])
  (:gen-class))

(def ^:private default-config-filename "cruler.edn")

(defn- run
  [dir options]
  (binding [log/*level* (if (:verbose options)
                          :info
                          :error)]
    (let [dir (io/file (or dir "."))
          [filepath config] (core/setup-config dir (or (:config options) default-config-filename))]
      (log/info "Loading config:" filepath)
      (report/reset-report-counter)
      (doseq [result (core/run-validators (:validators config) dir)]
        (log/info "\nValidating" (:validator result))
        (report/report result))
      (report/show-summary-report)
      (System/exit (if (report/has-success?) 0 1)))))

(def ^:private cli-options
  [["-c" "--config CONFIG" "Specify a configuration file (default: cruler.edn)"]
   ["-v" "--verbose" "Make cruler verbose during the operation"]
   ["-h" "--help" "Print help"]])

(defn- usage
  [options-summary]
  (->> ["Usage: cruler [<options>] [<directory>]"
        ""
        "Options:"
        options-summary]
       (string/join \newline)))

(defn- error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (string/join \newline errors)))

(defn- validate-args
  [args]
  (let [{:keys [options arguments errors summary]} (cli/parse-opts args cli-options)]
    (cond
      (:help options)
      {:exit-message (usage summary), :ok? true}

      errors
      {:exit-message (error-msg errors)}

      (<= (count arguments) 1)
      {:dir (first arguments), :options options}

      :else
      {:exit-message (usage summary)})))

(defn- exit
  [status msg]
  (if (zero? status)
    (println msg)
    (log/error msg))
  (System/exit status))

(defn -main [& args]
  (let [{:keys [dir options exit-message ok?]} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (try
        (run dir options)
        (catch Exception e
          (exit 1 (.getMessage e)))))))
