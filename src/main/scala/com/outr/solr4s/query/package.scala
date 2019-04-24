package com.outr.solr4s

import scala.language.implicitConversions

package object query {
  implicit class QueryField[T](field: Field[T]) {
    def ===(value: T): Query = TermQuery(
      value = QueryValue(value.toString),
      field = Some(field.name)
    )
  }

  implicit class QueryFieldList[T](field: Field[List[T]]) {
    def ===(value: T): Query = TermQuery(
      value = QueryValue(value.toString),
      field = Some(field.name)
    )
  }

  implicit def string2QueryValue(s: String): QueryValue = QueryValue(s)
  implicit def boolean2QueryValue(b: Boolean): QueryValue = QueryValue(b)
  implicit def int2QueryValue(i: Int): QueryValue = QueryValue(i)
  implicit def long2QueryValue(l: Long): QueryValue = QueryValue(l)
  implicit def double2QueryValue(d: Double): QueryValue = QueryValue(d)

  def and(queries: Query*): Query = GroupedQuery(Condition.And, queries.toList)
  def or(queries: Query*): Query = GroupedQuery(Condition.Or, queries.toList)
  def not(queries: Query*): Query = GroupedQuery(Condition.Not, queries.toList)
}