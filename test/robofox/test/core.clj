(ns robofox.test.core
  (:require [clojure.test :refer :all]
            [robofox.core :refer :all]
            [clj-webdriver.taxi :as taxi]))

(deftest find-clojure-clojurescript
  (testing "find-clojure-clojurescript"
    (let [_ (firefox-default)
          repos (github-repos "clojure")]
      (is (> (count repos) 0))
      (is (not (nil? (first (filter #(= (first %) "clojurescript") repos)))))
      (taxi/quit))))
