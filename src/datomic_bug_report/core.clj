(ns datomic-bug-report.core
  (:gen-class)
  (:require [datomic.api :as d]))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

;; Set correct datomic storage pwd
(def datomic-uri "datomic:free://localhost:4334/datomic-bug-report?password=foobar")
(def schema-tx
  [])

(comment
  ;; 1. Start local transactor

  ;; 2. Create database
  (d/create-database datomic-uri)

  ;; 3. Connect to database
  (def conn (d/connect datomic-uri))

  ;; 4. Create schema
  @(d/transact conn schema-tx)

  ;; Bug examples here:



  )
