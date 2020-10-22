(ns cruler.validators-test
  (:require [cruler.core :as core]
            [cruler.validators]
            [clojure.test :as t]))

(t/deftest start-of-file
  (let [validate (partial core/validate :cruler.validators/start-of-file)]
    (t/testing "start of file is not blank line"
      (t/is (empty? (:errors (validate '({:raw-content "ABC\nDEF" :file-path "a.txt"}))))))
    (t/testing "start of file is blank line"
      (t/testing "single file"
        (t/is (= '("b.txt")
                 (->> (validate '({:raw-content "\nABC\nDEF" :file-path "b.txt"}))
                      (:errors)
                      (map :file-path)))))
      (t/testing "multiple files"
        (t/is (= '("b.txt" "c.txt")
                 (->> (validate '({:raw-content "\nABC\nDEF" :file-path "b.txt"}
                                  {:raw-content "\nABC\nDEF" :file-path "c.txt"}))
                      (:errors)
                      (map :file-path)
                      (sort))))))
    (t/testing "data is nil"
      (t/is (empty? (:errors (validate nil)))))))

(t/deftest trailing-whitespace
  (let [validate (partial core/validate :cruler.validators/trailing-whitespace)]
    (t/testing "no trailing whitespaces"
      (t/is (empty? (:errors (validate '({:raw-content "ABC" :file-path "a.txt"}))))))
    (t/testing "trailing whitespaces"
      (t/testing "single file"
        (t/is (= '("b.txt")
                 (->> (validate '({:raw-content "ABC " :file-path "b.txt"}))
                      (:errors)
                      (map :file-path)))))
      (t/testing "multiple files"
        (t/is (= '("b.txt" "c.txt")
                 (->> (validate '({:raw-content "ABC " :file-path "b.txt"}
                                  {:raw-content "ABC " :file-path "c.txt"}))
                      (:errors)
                      (map :file-path)
                      (sort)))))
      (t/testing "data is nil"
        (t/is (empty? (:errors (validate nil))))))))

(t/deftest end-of-line
  (let [validate (partial core/validate :cruler.validators/end-of-file)]
    (t/testing "end of file is not blank line"
      (t/is (empty? (:errors (validate '({:raw-content "ABC\nDEF\n" :file-path "a.txt"}))))))
    (t/testing "end of file is blank line"
      (t/testing "single file"
        (t/is (= '("b.txt")
                 (->> (validate '({:raw-content "ABC\n\n" :file-path "b.txt"}))
                      (:errors)
                      (map :file-path)))))
      (t/testing "multiple files"
        (t/is (= '("b.txt" "c.txt")
                 (->> (validate '({:raw-content "ABC\n\n" :file-path "b.txt"}
                                  {:raw-content "ABC\n\n" :file-path "c.txt"}))
                      (:errors)
                      (map :file-path)
                      (sort)))))
      (t/testing "data is nil"
        (t/is (empty? (:errors (validate nil))))))))

(t/deftest blank-line
  (let [validate (partial core/validate :cruler.validators/blank-line)]
    (t/testing "no blank line"
      (t/is (empty? (:errors (validate '({:raw-content "ABC\nDEF\n" :file-path "a.txt"}))))))
    (t/testing "blank line"
      (t/testing "single file"
        (t/is (= '("b.txt")
                 (->> (validate '({:raw-content "ABC\n\nDEF\n" :file-path "b.txt"}))
                      (:errors)
                      (map :file-path)))))
      (t/testing "multiple files"
        (t/is (= '("b.txt" "c.txt")
                 (->> (validate '({:raw-content "ABC\n\nDEF\n" :file-path "b.txt"}
                                  {:raw-content "ABC\n\nDEF\n" :file-path "c.txt"}))
                      (:errors)
                      (map :file-path)
                      (sort)))))
      (t/testing "data is nil"
        (t/is (empty? (:errors (validate nil))))))))
