# datomic-bug-report

App for illustrating and reporting Datomic bugs. Active bug reports may be in
different branches. See the open PRs.

## Report 1 2018-11-22

Running against latest Datomic Free transactor 0.9.5703 (but same issue shows
when running against lastest Datomic Pro version 0.9.5786 too).

### Details

Two issues show up, but they might be somewhat related.

Suppose that a database function is used to ensure correct atomic updates. Such
a generic function could, for instance, take a query and arguments for a query
as function arguments. Based on the query results, the function may allow the
transaction or throw.

Using such a function, we discover two issues:

1. Queries containing `not` clause behave unexpectedly.
2. Queries that use rules that have `or` clause break.

See [src/datomic_bug_report/core.clj](src/datomic_bug_report/core.clj) for
example.

Since neither issue manifests when using the Datomic in-memory database, it
seems that the underlying cause might be something related to serialization on
the wire.
