(ns datomic-bug-report.core
  (:gen-class)
  (:require [datomic.api :as d]))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

;; Set correct datomic storage pwd
(def datomic-uri "datomic:free://localhost:4334/datomic-bug-report?password=foobar")

(def user-1-id #uuid "5bf6b524-7d05-4078-9df3-a93c4a7007c5")
(def user-2-id #uuid "5bf6b53d-efcf-4d25-a05a-bdeeeb972cab")
(def user-3-id #uuid "5bf6b796-d05e-4079-8fea-95f7ce2f6376")

(def schema-tx
  [{:db/ident :user/id
    :db/valueType :db.type/uuid
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "User id"}
   {:db/ident :user/type
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/doc "User type"}
   ;; A db function that runs a query that is passed as argument and throws if
   ;; the query matches anything.
   {:db/ident :tx/ensure-none
    :db/doc "Throws if given query returns any result(s)."
    :db/fn (d/function
            {:lang "clojure"
             :params '[db query args]
             :code '(let [r (apply datomic.api/q query db args)]
                      (if (or (not (seqable? r))
                              (seq r))
                        (throw (ex-info ":tx/ensure-none constraint-violation"
                                        {:query-result r}))
                        []))})}])

(defn create-users!
  [conn]
  @(d/transact conn [{:user/id user-1-id
                      :user/type :admin}
                     {:user/id user-2-id
                      :user/type :user}
                     {:user/id user-3-id
                      :user/type :user}]))

(def query-ok-users-in-set '[:find [?e ...]
                             :in $ ?ids
                             :where [?e :user/id ?id]
                                    [(contains? ?ids ?id)]])

;; Same as query-ok, but using a `not` clause
(def query-1-users-not-in-set '[:find [?e ...]
                                :in $ ?ids
                                :where [?e :user/id ?id]
                                       (not [(contains? ?ids ?id)])])

(def rules
  '[[(users-and-admins ?u)
     (or [?e :user/type :user]
         [?e :user/type :admin])]])

;; A query using `or` clause
(def query-2-users '[:find [?e ...]
                            :in $ %
                            :where [?e :user/id]
                                   (users-and-admins ?e)])

(comment
  ;; 1. Start local transactor

  ;; 2. Create database
  (d/create-database datomic-uri)

  ;; 3. Connect to database
  (def conn (d/connect datomic-uri))

  ;; 4. Create schema
  @(d/transact conn schema-tx)

  ;; 5. Create users
  (create-users! conn)

  ;; 6. Test queries
  ;; This will return 2 matching entity ids
  (d/q query-ok-users-in-set (d/db conn) #{user-1-id user-2-id})

  ;; This finds 2 matching entity
  (d/q query-1-users-not-in-set (d/db conn) #{user-1-id})

  ;; This returns all 3 users
  (d/q query-2-users (d/db conn) rules)

  ;; 7. Transact with db fn :tx/ensure-none This uses the query-ok, which
  ;; behaves as expected, and the :tx/ensure-none throws constraint-violation
  ;; error, as expected.
  @(d/transact conn
               [[:tx/ensure-none
                 query-ok-users-in-set
                 [#{user-1-id user-2-id}]]])

  ;; BUG HERE:
  ;; 8. Transact with db fn :tx/ensure-none
  ;; This should throw, since the query matches
  ;; Instead, it transacts successfully
  @(d/transact conn
               [[:tx/ensure-none
                 query-1-users-not-in-set
                 [#{user-1-id}]]])

  ;; BUG 2 here: 9. `or` query inside a rule passed to db fn does not work and
  ;; throws error about the `or` clause.
  @(d/transact conn
               [[:tx/ensure-none
                 query-2-users
                 [rules]]])


  )
