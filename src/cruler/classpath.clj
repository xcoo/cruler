(ns cruler.classpath
  (:refer-clojure :exclude [add-classpath])
  (:require [cemerick.pomegranate :as pomegranate]
            [clojure.java.io :as io])
  (:import (clojure.lang DynamicClassLoader)))

(defn ensure-dynamic-classloader []
  (let [thread (Thread/currentThread)
        context-class-loader (.getContextClassLoader thread)
        compiler-class-loader (.getClassLoader clojure.lang.Compiler)]
    (when-not (instance? DynamicClassLoader context-class-loader)
      (.setContextClassLoader
       thread (DynamicClassLoader. (or context-class-loader
                                       compiler-class-loader))))))

(defn add-classpaths
  [base paths]
  (doseq [path paths]
    (pomegranate/add-classpath (io/file base path))))

(defn add-deps [deps]
  (pomegranate/add-dependencies
   :coordinates deps
   :repositories (merge cemerick.pomegranate.aether/maven-central
                        {"clojars" "https://clojars.org/repo"})))
