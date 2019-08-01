package com.outr.solr4s

import com.outr.solr4s.admin.{Direction, Sort}
import com.outr.solr4s.query.{Operator, Query, QueryValue, TermQuery}

import scala.language.implicitConversions

case class Field[T](name: String,
                    `type`: FieldType,
                    default: String = "",
                    indexed: Boolean = true,
                    stored: Boolean = true,
                    docValues: Boolean = false,
                    sortMissingFirst: Boolean = false,
                    sortMissingLast: Boolean = false,
                    multiValued: Boolean = false,
                    uninvertible: Boolean = true,
                    omitNorms: Option[Boolean] = None,
                    omitTermFreqAndPositions: Option[Boolean] = None,
                    omitPositions: Option[Boolean] = None,
                    termVectors: Boolean = false,
                    termPositions: Boolean = false,
                    termOffsets: Boolean = false,
                    termPayloads: Boolean = false,
                    required: Boolean = false,
                    useDocValuesAsStored: Boolean = true,
                    large: Boolean = false) {
  def apply(direction: Direction): Sort = Sort(name, direction)
  def ascending: Sort = apply(Direction.Ascending)
  def descending: Sort = apply(Direction.Descending)
  def filter(value: QueryValue,
             proximity: Option[Int] = None,
             boost: Option[Double] = None,
             constantScore: Option[Double] = None,
             operator: Operator = Operator.Should): Query = TermQuery(
    value = value,
    field = Some(name),
    proximity = proximity,
    boost = boost,
    constantScore = constantScore,
    operator = operator
  )
}

object Field {
  implicit def field2String[T](field: Field[T]): String = field.name
}