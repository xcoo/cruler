(ns cruler.main
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.tools.cli :as cli]
            [cruler.classpath :as classpath]
            [cruler.config :as config]
            [cruler.core :as core]
            [cruler.log :as log])
  (:gen-class))

(def ^:private default-config-filename "cruler.edn")

(defn- load-config
  [dir path]
  (let [file (io/file (or path default-config-filename))
        file (if (.isAbsolute file)
               file
               (io/file dir file))]
    (log/info "Loading config:" (.getPath file))
    (config/load-config file)))

(defn- run
  [dir options]
  (binding [log/*level* (if (:verbose options)
                          :info
                          :error)]
    (let [dir (io/file (or dir "."))
          config (load-config dir (:config options))]
      (classpath/ensure-dynamic-classloader)
      (classpath/add-classpaths dir (:paths config))
      (classpath/add-deps (:deps config))
      (let [summary (core/run-validators (:validators config) dir)]
        (System/exit (if (zero? (:fail summary)) 0 1))))))

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
