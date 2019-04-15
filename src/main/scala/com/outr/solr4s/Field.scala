package com.outr.solr4s

import com.outr.solr4s.admin.{Direction, Sort}

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
}