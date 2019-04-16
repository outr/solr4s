package com.outr.solr4s

package object query {
  implicit class QueryField[T](field: Field[T]) {
    def ===(value: T): Query = TermQuery(
      value = QueryValue(value.toString),
      field = Some(field.name)
    )
  }

  def and(queries: Query*): Query = GroupedQuery(Condition.And, queries.toList)
  def or(queries: Query*): Query = GroupedQuery(Condition.Or, queries.toList)
  def not(queries: Query*): Query = GroupedQuery(Condition.Not, queries.toList)
}