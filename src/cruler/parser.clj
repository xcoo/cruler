(ns cruler.parser
  (:require [clojure.data.csv :as csv]
            [clj-yaml.core :as yaml]
            [clojure.string :as string]))

(defn parse-csv [s]
  (->> (csv/read-csv s)
       (map-indexed vector)
       (map (fn [[line coll]]
              (with-meta coll
                {:line line
                 :column 0
                 :children-starts (mapv (fn [column]
                                          {:line line
                                           :column column})
                                        (range (count coll)))})))))

(defn parse-text [s]
  (->> (string/split-lines s)
       (map-indexed (fn [line coll-str] [line (string/split coll-str #" ")]))
       (map (fn [[line coll]]
              (with-meta coll
                {:line line
                 :column 0
                 :children-starts (mapv (fn [column]
                                          {:line line
                                           :column column})
                                        (range (count coll)))})))))

(defn- process-yaml-meta [marked]
  (let [start (:start marked)
        unmark (:unmark marked)]
    (cond
      (map? unmark) (with-meta (into {}
                                     (map (fn [[k v]]
                                            [(keyword (:unmark k))
                                             (process-yaml-meta v)])
                                          unmark))
                      (assoc start
                             :children-starts (into {}
                                                    (map (fn [[k _]]
                                                           [(keyword (:unmark k))
                                                            (:start k)])
                                                         unmark))))
      (seq? unmark) (with-meta (mapv process-yaml-meta unmark)
                      (assoc start :children-starts (mapv :start unmark)))
      :else unmark)))

(defn parse-yaml [s]
  (let [data (try
               (yaml/parse-string s :mark true)
               (catch Throwable e e))]
    (if (instance? Throwable data)
      data
      (process-yaml-meta data))))
