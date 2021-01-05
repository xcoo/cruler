(ns cruler.core
  (:require [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [clojure.string :as string]
            [cruler.spec-parser :as sp]
            [cruler.parser :as parser]
            [cruler.config :as config]
            [cruler.classpath :as classpath]
            [io.aviso.ansi :as ansi]))

(defmulti validate
  "Validates `data`, returning a result map.

  `data` is a vector containing maps. Each map has a file information as
  follows:

    {:file-path       The file path relative to the base directory.

     :file-type       The file type, any of `:csv`, `:text`, and `:yaml`.

     :raw-content     The raw content of the file.

     :parsed-content  The parsed data of `:raw-content`. The structure of
                      the data depends on `:file-type`.}

  `validate` multimethods returns the result map consisting of the following
  keys:

    {:errors   A vector of maps indicating errors. Each error map consists of

                 {:file-path    The file path in which an error occurs.

                  :error-value  The error value.

                  :error-block  A block in `:parsed-content` containing the
                                error location.

                  :error-keys   Keys in `:error-block` for indicating the
                                specific error location.}

               If there are no errors, set `nil` or an empty sequence.

     :message  An optional error message.}"
  {:arglists '([key data])}
  (fn [key _] key))

(defn- filter-files [dir xs]
  (->> (file-seq (io/file dir))
       (filter (fn [file]
                 (some #(re-find % (.getPath file)) xs)))
       (sort-by #(.getPath %))))

(defn- error-lines
  [error-block error-keys]
  (let [children-starts (:children-starts (meta error-block))]
    (->> (keep #(get children-starts %) error-keys)
         (map :line)
         distinct)))

(defn- error-block-lines
  [error-block error-keys]
  (when-let [lines (seq (error-lines error-block error-keys))]
    (map inc lines)))

(defn- error-block-message
  [lines]
  (if lines
    (str "  line: " (string/join "," lines))
    "  line: unknown"))

(defn- pprint-str [x]
  (pprint/write x :stream nil))

(defn- indent [s n]
  (->> (string/split-lines s)
       (map #(str (apply str (repeat n " ")) %))
       (string/join \newline)))

(defn- build-error-str [err]
  (str (if (symbol? (:pred err))
         (str (:pred err))
         (str (apply list (:pred err))))
       \newline
       (indent (pprint-str (:val err)) 4)))

(defn- error-value-message
  [error-value]
  (if (sp/spec-problem? error-value)
    (if-let [readable-error (sp/readable-error error-value)]
      (str (apply str readable-error) \newline
           (indent (pprint-str (:val error-value)) 4))
      (build-error-str error-value))
    (pprint-str error-value)))

(defn- calc-range
  [line-num content num]
  (let [min-line-num (min (count content) (max (- line-num num) 1))
        max-line-num (min (+ line-num num) (count content))]
    {:min-line min-line-num :max-line max-line-num}))

(defn- take-range
  [content min-line-num max-line-num]
  (->> (drop (dec min-line-num) content)
       (take (inc (- max-line-num min-line-num)))))

(defn- preview
  [content line-num preview-num colorize?]
  (let [{:keys [min-line max-line]} (calc-range line-num content preview-num)
        line-fmt (str "%" (count (str max-line)) "s")]
    (map (fn [l s]
           (if (= l line-num)
             (cond-> (str (format line-fmt l) " " s)
               colorize? ansi/yellow)
             (str (cond-> (format line-fmt l)
                    colorize? ansi/green) " " s)))
         (iterate inc min-line)
         (take-range content min-line max-line))))

(def ^:private preview-range 2)
(defn- build-preview
  [raw-content indices colorize?]
  (let [content (string/split-lines raw-content)
        valid-indices (->> (remove nil? indices)
                           (remove zero?)
                           (remove #(< (count content) %)))]
    (str "  preview:\n-----\n"
         (->> valid-indices
              (map #(preview content % preview-range colorize?))
              (map #(string/join \newline %))
              (string/join "\n-----\n"))
         "\n-----")))

(defn- build-error-message
  [errors message data]
  (let [message (when message
                  (str message \newline))]
    (->> errors
         (mapcat (fn [{:keys [file-path error-block error-keys error-value]}]
                   (let [indices (if (seq error-block)
                                   (error-block-lines error-block error-keys)
                                   error-keys)
                         raw-content (->> (filter #(= file-path (:file-path %)) data)
                                          first
                                          :raw-content)]
                     [""
                      file-path
                      (when error-value
                        (str "  error: " (error-value-message error-value)))
                      (when (seq error-keys)
                        (str (error-block-message indices) "\n"
                             (when (and indices raw-content)
                               ;; TODO I want to switch showing colors by config
                               (build-preview raw-content indices true))))])))
         (remove nil?)
         (string/join \newline)
         (str message))))

(defn- build-result
  [{:keys [errors message]} key data]
  {:type (if (empty? errors) :pass :fail)
   :validator key
   :message (build-error-message errors message data)})

(defn- file-type [file]
  (condp re-find (.getName file)
    #"\.csv$"   :csv
    #"\.ya?ml$" :yaml
    :text))

(defn- build-data1 [file]
  (let [file-type (file-type file)
        raw-content (slurp file)]
    {:file-path (.getPath file)
     :file-type file-type
     :raw-content raw-content
     :parsed-content (case file-type
                       :csv (parser/parse-csv raw-content)
                       :text (string/split-lines raw-content)
                       :yaml (parser/parse-yaml raw-content))}))

(defn run-validators
  [validators base-dir]
  (for [[rule patterns] validators]
    (do
      (require (symbol (namespace rule)))
      (let [data (->> (map re-pattern patterns)
                      (filter-files base-dir)
                      (mapv build-data1))
            result (-> (validate rule data)
                       (build-result rule data))]
        result))))

(defn run-validators-single-file
  [validators filepath]
  (let [file (io/file filepath)
        data [(build-data1 file)]]
    (for [[rule _] validators]
      (do
        (require (symbol (namespace rule)))
        (let [result (-> (validate rule data)
                         (build-result rule data))]
          result)))))

(defn setup-config
  [dir filepath]
  (let [dir (io/file (or dir "."))
        [absolute-filepath config] (config/load-config dir filepath)]
    (classpath/ensure-dynamic-classloader)
    (classpath/add-classpaths dir (:paths config))
    (classpath/add-deps (:deps config))
    [absolute-filepath config]))
