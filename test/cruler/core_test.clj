(ns cruler.core-test
  (:require [clojure.string :as string]
            [clojure.test :as t]
            [cruler.core :as core]
            [cruler.parser :as parser]))

(def sample-yaml
  "- item-1: foo
  item-2: bar")

(def sample-csv
  "1a,1b,1c
2a,2b")

(t/deftest build-error-test
  (let [build-error-message #'core/build-error-message]
    (t/testing "show message"
      (t/is (string/includes? (build-error-message nil "test" nil) "test")))
    (t/testing "show file-path, error-value"
      (let [errors [{:file-path "foo.txt"
                     :error-value "bar"}]
            msg (build-error-message errors nil nil)]
        (t/is (string/includes? msg "foo.txt"))
        (t/is (string/includes? msg "error: \"bar\""))))
    (t/testing "line-num if only error-keys"
      (let [errors [{:error-block nil
                     :error-keys [1 2 3]}]
            msg (build-error-message errors nil nil)]
        (t/is (string/includes? msg "line: 1,2,3"))))
    (t/testing "with error-block"
      (let [parsed-yaml-block (first (parser/parse-yaml sample-yaml))
            parsed-csv-block (first (parser/parse-csv sample-csv))]
        (t/are [error-block error-keys expected-msg] (string/includes?
                                                      (build-error-message [{:error-block error-block
                                                                             :error-keys error-keys}]
                                                                           nil
                                                                           nil)
                                                      expected-msg)
          parsed-yaml-block [:item-1 :item-2] "line: 1,2"
          parsed-yaml-block [:foo] "line: unknown"
          parsed-csv-block [0] "line: 1"
          parsed-csv-block [10] "line: unknown")))
    (t/testing "preview"
      (let [data [{:file-path "foo.txt"
                   :raw-content "abc"}]
            errors [{:file-path (:file-path (first data))
                     :error-keys [1]}]
            msg (build-error-message errors nil data)]
        (t/is (string/includes? msg "preview"))))))

(t/deftest build-preview-test
  (let [build-preview #'core/build-preview
        content "foo\nbar\nfizz\nbazz\nqux\nquux"]
    (t/testing "one line-num"
      (t/are [preview line-num] (= preview (build-preview content line-num false))
        "  preview:\n-----\n\n-----"                                    nil
        "  preview:\n-----\n\n-----"                                    [0]
        "  preview:\n-----\n1 foo\n2 bar\n3 fizz\n-----"                [1]
        "  preview:\n-----\n1 foo\n2 bar\n3 fizz\n4 bazz\n5 qux\n-----" [3]
        "  preview:\n-----\n4 bazz\n5 qux\n6 quux\n-----"               [6]
        "  preview:\n-----\n\n-----"                                    [7]))
    (t/testing "multi line-nums"
      (t/are [preview line-num] (= preview (build-preview content line-num false))
        "  preview:\n-----\n1 foo\n2 bar\n3 fizz\n-----\n1 foo\n2 bar\n3 fizz\n4 bazz\n-----" [1 2]
        "  preview:\n-----\n1 foo\n2 bar\n3 fizz\n-----\n4 bazz\n5 qux\n6 quux\n-----"        [1 6]))))

(t/deftest run-validators-test
  (let [run-validators #'core/run-validators]
    (t/testing "show type and message of validator in results"
      (let [validators {:cruler.validators/start-of-file ["sample/[\\w-]+\\.(csv|yml)$"]
                        :cruler.validators/trailing-whitespace ["sample/[\\w-]+\\.(csv|yml)$"]
                        :cruler.validators/end-of-file ["sample/[\\w-]+\\.(csv|yml)$"]
                        :cruler.validators/blank-line ["sample/[\\w-]+\\.csv$"]}
            base-dir "test/cruler/resources/"
            results (run-validators validators base-dir)]
        (t/is (= (count results) 4))
        (let [result (first (filter #(= (:validator %) :cruler.validators/start-of-file) results))]
          (t/is (= (:type result) :fail))
          (t/is (string/includes? (:message result) "failure.csv"))
          (t/is (string/includes? (:message result) "failure.yml")))
        (let [result (first (filter #(= (:validator %) :cruler.validators/trailing-whitespace) results))]
          (t/is (= (:type result) :pass)))
        (let [result (first (filter #(= (:validator %) :cruler.validators/end-of-file) results))]
          (t/is (= (:type result) :pass)))
        (let [result (first (filter #(= (:validator %) :cruler.validators/blank-line) results))]
          (t/is (= (:type result) :fail))
          (t/is (string/includes? (:message result) "failure.csv")))))))
