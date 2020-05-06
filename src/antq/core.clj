(ns antq.core
  (:gen-class)
  (:require
   [ancient-clj.core :as ancient]
   [antq.dep.boot :as dep.boot]
   [antq.dep.clojure :as dep.clj]
   [antq.dep.leiningen :as dep.lein]
   [antq.dep.pom :as dep.pom]
   [antq.dep.shadow :as dep.shadow]
   [antq.ver :as ver]
   [antq.ver.java]
   [clojure.pprint :as pprint]
   [version-clj.core :as version]))

(def default-skip-artifacts
  #{"org.clojure/clojure"})

(def default-repos
  {"central" "https://repo1.maven.org/maven2/"
   "clojars" "https://repo.clojars.org/"})

(defn skip-artifacts?
  [dep]
  (contains? default-skip-artifacts (:name dep)))

(defn using-release-version?
  [dep]
  (contains? #{"RELEASE"} (:version dep)))

(defn latest?
  [dep]
  (and (:version dep)
       (:latest-version dep)
       (zero?  (version/version-compare
                (:version dep)
                (:latest-version dep)))))

(defn outdated-deps
  [deps]
  (->> deps
       (remove skip-artifacts?)
       (remove using-release-version?)
       (pmap #(assoc % :latest-version (ver/get-latest-version %)))
       (remove latest?)))

(defn- compare-deps
  [x y]
  (let [prj (.compareTo (:file x) (:file y))]
    (if (zero? prj)
      (.compareTo (:name x) (:name y))
      prj)))

(defn skip-duplicated-file-name
  [sorted-deps]
  (loop [[dep & rest-deps] sorted-deps
         last-file nil
         result []]
    (if-not dep
      result
      (if (= last-file (:file dep))
        (recur rest-deps last-file (conj result (assoc dep :file "")))
        (recur rest-deps (:file dep) (conj result dep))))))

(defn print-deps
  [deps]
  (if (seq deps)
    (->> deps
         (sort compare-deps)
         skip-duplicated-file-name
         (map #(update % :latest-version (fnil identity "Failed to fetch")))
         (pprint/print-table [:file :name :version :latest-version]))
    (println "All dependencies are up-to-date."))
  deps)

(defn exit
  [outdated-deps]
  (System/exit (if (seq outdated-deps) 1 0)))

(defn -main
  []
  (let [deps (concat (dep.boot/load-deps)
                     (dep.clj/load-deps)
                     (dep.pom/load-deps)
                     (dep.shadow/load-deps)
                     (dep.lein/load-deps))]
    (if (seq deps)
      (-> deps
          outdated-deps
          print-deps
          exit)
      (do (println "No project file")
          (System/exit 1)))))
