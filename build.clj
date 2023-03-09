(ns build
  (:require [clojure.tools.build.api :as b]))

(def lib 'xcoo/cruler)
(def version "1.2.2")

(def basis (b/create-basis {:project "deps.edn"}))
(def src-pom "dev-resources/template-pom.xml")
(def class-dir "target/classes")

(def default-jar-file (format "target/%s-%s.jar" (name lib) version))

(def main-class 'cruler.main)
(def default-uber-file (format "target/%s-%s-standalone.jar" (name lib) version))

(defn clean [_]
  (b/delete {:path "target"}))

(defn jar
  [{:keys [jar-file]}]
  (b/write-pom {:basis basis
                :src-pom src-pom
                :class-dir class-dir
                :lib lib
                :version version
                :src-dirs ["src"]})
  (b/copy-dir {:src-dirs ["src" "resources"]
               :target-dir class-dir})
  (b/jar {:class-dir class-dir
          :jar-file (str (or jar-file default-jar-file))}))

(defn uber
  [{:keys [uber-file]}]
  (clean nil)
  (b/copy-dir {:src-dirs ["src" "resources"]
               :target-dir class-dir})
  (b/compile-clj {:basis basis
                  :src-dirs ["src"]
                  :class-dir class-dir})
  (b/uber {:class-dir class-dir
           :uber-file (str (or uber-file default-uber-file))
           :basis basis
           :main main-class}))

(defn get-version [_]
  (println version))
