(ns robofox.core
  (:import [org.openqa.selenium.firefox FirefoxBinary FirefoxProfile FirefoxDriver]
           [java.io File])
  (:require [clojure.string :as string]
            [clojure.pprint :as pp]
            [clj-webdriver.driver :as driver]
            [clj-webdriver.taxi :as taxi]))

;; most users will want to initialize firefox with this function
(defn firefox-default
  "Initialize default Firefox profile"
  []
  (taxi/set-driver! {:browser :firefox}))

;; alternate function for customizing which browser and profile you need
(defn firefox-profile
  "Set Firefox driver with specific browser path and profile"
  [browser profile]
  (let [ffpath (File. browser)
        ffbin (FirefoxBinary. ffpath)
        ffprofdir (File. profile)
        ffprof (FirefoxProfile. ffprofdir)
        ffdrv (FirefoxDriver. ffbin ffprof)
        firefox (driver/init-driver {:webdriver ffdrv})]
    (taxi/set-driver! firefox)))

(defn start-firefox
  "Start firefox (with optional browser path and profile)"
  [browser profile]
  (if (and browser profile)
    (firefox-profile browser profile)
    (firefox-default)))

(defn link-text-href
  "return the [text href] for the link a"
  [a]
  [(taxi/attribute a :text) (taxi/attribute a :href)])

(defn show-links
  "Return the first count links on the current page"
  [count]
  (let [hrefs (taxi/find-elements {:tag :a})
        links (mapv link-text-href (take count hrefs))]
    links))

(defn repo-name-desc
  "Returns repo [name desc] given [language name optional-forked desc update]"
  [details]
  (let [name (nth details 1 "")
        forked (nth details 2 "")
        desc (if (.startsWith forked "forked") (nth details 3 "") forked)
        description (if (.startsWith desc "Updated") "" desc)]
    [name description]))

;; BUG only finds repos on the *first page*
(defn github-repo-list
  "list repositories for the valid github username (or organization)"
  [username]
  (if (taxi/exists? "div.org-header-info") ;; organization?
    (let [repo-items (taxi/find-elements-under
                       "div#org-repositories" {:tag :div})
          repo-lines (remove #(empty? %)
                       (map #(taxi/attribute % :text) repo-items))
          repo-fields (map #(string/split-lines %) repo-lines)
          repo-details (remove #(or (= (count %) 1) (= (count %1) 6))
                         repo-fields)
          repos (mapv repo-name-desc repo-details)]
      repos)
    (let [repositories (str "/" username "?tab=repositories")
          _ (taxi/click (taxi/find-element {:tag :a :href repositories}))
          _ (taxi/wait-until #(taxi/exists? "ul.repo-list"))
          repo-items (taxi/find-elements-under "ul.repo-list" {:tag :li})
          repo-details (map
                         #(string/split-lines (taxi/attribute % :text))
                         repo-items)
          repos (mapv repo-name-desc repo-details)]
      repos)))

(defn github-repos
  "Returns a vector of [name desc] for github repos by username"
  [username]
  (let [github-page (str "https://github.com/" username)]
    (taxi/to github-page)
    (if (= (taxi/title) "Page not found · GitHub")
      (println "Sorry, github user not found:" username)
      (github-repo-list username))))

;; usage: lein run username [browser-path profile]
;; display the github repositories for username
(defn -main [& args]
  (let [username (nth args 0 "unknown")
        browser (nth args 1 nil)
        profile (nth args 2 nil)]
    (println "Starting Firefox...")
    (start-firefox browser profile)
    (println "Finding github repositories for:" username)
    (let [repos (github-repos username)]
      (if (nil? repos)
        (println "no repositories found for" username ":(")
        (pp/pprint repos)))
    (println "Stopping firefox...")
    (taxi/quit)))
