(ns cruler.spec-parser-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :as t]
            [cruler.spec-parser :as sp]
            [phrase.alpha :as p]))

(s/def ::integer integer?)
(s/def ::string string?)
(s/def ::req-un (s/keys :req-un [::a]))
(s/def ::contains #(contains? % :my-key))
(s/def ::apply-distinct #(apply distinct? %))
(s/def ::apply-distinct-map #(apply distinct? (map :a %)))
(s/def ::re-matches #(re-matches #"A\d+" %))
(s/def ::coll-of-int (s/coll-of integer?))
(s/def ::distincted (s/coll-of string? :distinct true))
(s/def ::rules (s/keys :req-un [::integer ::string]))
(s/def ::nested (s/coll-of ::rules))

(t/deftest defphraser-test
  (let [shown-msg (fn [def-name value]
                    (p/phrase-first {} def-name value))]
    (t/testing "integer?"
      (t/is (nil? (shown-msg ::integer 1)))
      (t/is (= "Should be integer" (shown-msg ::integer "a"))))
    (t/testing "string?"
      (t/is (nil? (shown-msg ::string "a")))
      (t/is (= "Should be string" (shown-msg ::string 1))))
    (t/testing "req-un"
      (t/is (nil? (shown-msg ::req-un {:a 1})))
      (t/is (= "Missing key: a" (shown-msg ::req-un {:b 1}))))
    (t/testing "contains?"
      (t/is (nil? (shown-msg ::contains {:my-key 1})))
      (t/is (= "Missing key: my-key" (shown-msg ::contains {:a 1}))))
    (t/testing "apply distinct?"
      (t/is (nil? (shown-msg ::apply-distinct [1 2])))
      (t/is (= "Should be distincted" (shown-msg ::apply-distinct [1 1]))))
    (t/testing "apply-distinct-map"
      (t/is (nil? (shown-msg ::apply-distinct-map [{:a 1} {:a 2}])))
      (t/is (= "Should be distincted: :a" (shown-msg ::apply-distinct-map [{:a 1} {:a 1}]))))
    (t/testing "re-maches"
      (t/is (nil? (shown-msg ::re-matches "A1")))
      (t/is (= "Should match regex: A\\d+" (shown-msg ::re-matches "B1"))))
    (t/testing "coll-of"
      (t/is (nil? (shown-msg ::coll-of-int [1 2 3])))
      (t/is (= "Should be a collection" (shown-msg ::coll-of-int 1))))
    ;; (t/testing "distincted"
    ;;   (t/is (nil? (shown-msg ::distincted ["a" "b"])))
    ;;   (t/is (= "Should be distincted" (shown-msg ::distincted ["a" "a"]))))
    (t/testing "nested"
      (t/is (nil? (shown-msg ::nested [{:string "a" :integer 1}])))
      (t/is (= "Missing key: integer" (shown-msg ::nested [{:string "a"}])))
      (t/is (= "Should be integer" (shown-msg ::nested [{:string "a" :integer "a"}])))
      (t/is (= (or "Should be string" "Should be integer") (shown-msg ::nested [{:string 1 :integer "a"}]))))))

(t/deftest spec-problem-test
  (t/testing "return true if spec-problem"
    (let [build-problem (fn [x]
                          (first (::s/problems (s/explain-data ::integer x))))
          prob1 (build-problem "a")
          prob2 (build-problem false)]
      (t/is (sp/spec-problem? prob1))
      (t/is (sp/spec-problem? prob2))))
  (t/testing "return false if not spec-problem"
    (let [prob1 "problem"
          prob2 nil]
      (t/is (not (sp/spec-problem? prob1)))
      (t/is (not (sp/spec-problem? prob2))))))

(t/deftest readable-error-test
  (let [build-problem (fn [key value]
                        (first (::s/problems (s/explain-data key value))))]
    (t/testing "defined-error-msg"
      (s/def ::dummy-spec #(= "dummy" %))
      (sp/defmsg ::dummy-spec "my message")
      (let [prob (build-problem ::dummy-spec "a")]
        (t/is (= "my message" (sp/readable-error prob)))))
    (t/testing "phrase-unhandled-msg"
      (let [prob (build-problem ::distincted ["a" "a"])]
        (t/is (= "Should be distincted" (sp/readable-error prob)))))
    (t/testing "no match"
      (s/def ::no-match-spec #(= "TEST" %))
      (let [prob (build-problem ::no-match-spec "a")]
        (t/is (not (sp/readable-error prob)))))))
