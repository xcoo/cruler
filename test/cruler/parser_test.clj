(ns cruler.parser-test
  (:require [clojure.test :refer [are deftest is]]
            [cruler.parser :as parser]))

(def sample-csv
  "1a,1b,1c
2a,2b

4a")

(deftest parse-csv-test
  (let [parsed (parser/parse-csv sample-csv)]
    (is (= parsed [["1a" "1b" "1c"]
                   ["2a" "2b"]
                   [""]
                   ["4a"]]))
    (are [n m] (= (meta (nth parsed n)) m)
      0 {:line 0, :column 0, :children-starts [{:line 0, :column 0}
                                               {:line 0, :column 1}
                                               {:line 0, :column 2}]}
      1 {:line 1, :column 0, :children-starts [{:line 1, :column 0}
                                               {:line 1, :column 1}]}
      2 {:line 2, :column 0, :children-starts [{:line 2, :column 0}]}))

  (is (= (parser/parse-csv "") []))

  (is (thrown? Exception (parser/parse-csv nil))))

(def sample-text
  "I have a pen
I love you

gacha")

(deftest parse-text-test
  (let [parsed (parser/parse-text sample-text)]
    (is (= parsed [["I" "have" "a" "pen"]
                   ["I" "love" "you"]
                   [""]
                   ["gacha"]]))
    (are [n m] (= (meta (nth parsed n)) m)
      0 {:line 0, :column 0, :children-starts [{:line 0, :column 0}
                                               {:line 0, :column 1}
                                               {:line 0, :column 2}
                                               {:line 0, :column 3}]}
      1 {:line 1, :column 0, :children-starts [{:line 1, :column 0}
                                               {:line 1, :column 1}
                                               {:line 1, :column 2}]}
      2 {:line 2, :column 0, :children-starts [{:line 2, :column 0}]}
      3 {:line 3, :column 0, :children-starts [{:line 3, :column 0}]}))

  (is (= (parser/parse-text "") [[""]]))

  (is (thrown? Exception (parser/parse-text nil))))

(def sample-yaml
  "- a: foo
  b:
    - c: 1
      d: true
    - c: 2
      d: false

- a: bar
  b:
    - c: 3
      d: false")

(deftest parse-yaml-test
  (let [parsed (parser/parse-yaml sample-yaml)]
    (is (= parsed [{:a "foo"
                    :b [{:c 1, :d true} {:c 2, :d false}]}
                   {:a "bar"
                    :b [{:c 3, :d false}]}]))
    (are [ks m] (= (meta (get-in parsed ks)) m)
      [0]      {:line 0, :index 2, :column 2
                :children-starts {:a {:line 0, :index 2, :column 2}
                                  :b {:line 1, :index 11, :column 2}}}
      [0 :b]   {:line 2, :index 18, :column 4
                :children-starts [{:line 2, :index 20, :column 6}
                                  {:line 4, :index 45, :column 6}]}
      [1 :b 0] {:line 9, :index 86, :column 6
                :children-starts {:c {:line 9, :index 86, :column 6}
                                  :d {:line 10, :index 97, :column 6}}}))

  (is (nil? (parser/parse-yaml "")))
  (is (nil? (parser/parse-yaml "# comment only\n# yaml file")))

  (is (thrown? Exception (parser/parse-yaml nil))))
