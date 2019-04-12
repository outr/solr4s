package com.outr.solr4s

package object query {
  implicit class QueryField[T](field: Field[T]) {
    def ===(value: T): Query = TermQuery(
      value = QueryValue(value.toString),
      field = Some(field.name)
    )
  }
}