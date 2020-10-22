(ns cruler.spec-parser
  (:require [phrase.alpha :as p]))

;; ref: https://github.com/bhb/expound/blob/master/src/expound/specs.cljc
(p/defphraser integer? [_ _] "Should be integer")
(p/defphraser string? [_ _] "Should be string")
(p/defphraser boolean? [_ _] "Should be either true or false")
(p/defphraser bytes? [_ _] "Should be an array of bytes")
(p/defphraser double? [_ _] "Should be a double")
(p/defphraser ident? [_ _] "Should be an identifier (a symbol or keyword)")
(p/defphraser indexed? [_ _] "Should be an indexed collection")
(p/defphraser int? [_ _] "Should be an integer")
(p/defphraser keyword? [_ _] "Should be a keyword")
(p/defphraser map? [_ _] "Should be a map")
(p/defphraser nat-int? [_ _] "Should be an integer equal to, or greater than, zero")
(p/defphraser neg-int? [_ _] "Should be a negative integer")
(p/defphraser pos-int? [_ _] "Should be a positive integer")
(p/defphraser qualified-ident? [_ _] "Should be an identifier (a symbol or keyword) with a namespace")
(p/defphraser qualified-keyword? [_ _] "Should be a keyword with a namespace")
(p/defphraser qualified-symbol? [_ _] "Should be a symbol with a namespace")
(p/defphraser seqable? [_ _] "Should be a seqable collection")
(p/defphraser simple-ident? [_ _] "Should be an identifier (a symbol or keyword) with no namespace")
(p/defphraser simple-keyword? [_ _] "Should be a keyword with no namespace")
(p/defphraser simple-symbol? [_ _] "Should be a symbol with no namespace")
(p/defphraser symbol? [_ _] "Should be a symbol")
(p/defphraser uri? [_ _] "Should be a URI")
(p/defphraser uuid? [_ _] "Should be a UUID")
(p/defphraser vector? [_ _] "Should be a vector")

;; NOTE: This defphraser does not triggerd in the case of
;; `(s/def ::distincted (s/coll-of string? :distinct true))`
;; (p/defphraser distinct? [_ _] "Should be distincted")
(p/defphraser #(apply distinct? %) [_ _] "Should be distincted")
(p/defphraser #(re-matches re %) [_ _ re] (str "Should match regex: " re))
(p/defphraser coll? [_ _] "Should be a collection")
(p/defphraser #(contains? % key) [_ _ key] (str "Missing key: " (name key)))
(p/defphraser #(apply distinct? (map x %)) [_ _ x] (str "Should be distincted: " x))

(def ^:private defined-spec-error-msg
  (atom {}))

(defn- defined-error-msg
  [prob]
  (let [spec-name (last (:via prob))]
    (get @defined-spec-error-msg spec-name)))

;; Some spec-problems does not contain `clojure.core` namespace
;; then `phrase` cannot catch those problems
;; cf: https://github.com/alexanderkiel/phrase/issues/22
;; cf: https://clojure.atlassian.net/browse/CLJ-2168
(defn- phrase-unhandled-msg
  [problem]
  (case (keyword (:pred problem))
    :distinct? "Should be distincted"
    nil))

;; problem-map has at least :path :pred and :val keys
;; describing the predicate and the value that failed at that path.
;; cf: https://clojuredocs.org/clojure.spec.alpha/explain-data
(defn spec-problem? [prob]
  (and (coll? prob)
       (contains? prob :path)
       (contains? prob :pred)
       (contains? prob :val)))

(defn defmsg
  [k error-message]
  (swap! defined-spec-error-msg assoc k error-message))

(defn readable-error
  [problem]
  (or (defined-error-msg problem)
      (p/phrase {} problem)
      (phrase-unhandled-msg problem)))
