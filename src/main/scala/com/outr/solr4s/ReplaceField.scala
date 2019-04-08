package com.outr.solr4s

import io.circe.Json
import profig.JsonUtil

case class ReplaceField(name: String,
                        `type`: String,
                        default: Option[String],
                        indexed: Option[Boolean],
                        stored: Option[Boolean],
                        docValues: Option[Boolean],
                        sortMissingFirst: Option[Boolean],
                        sortMissingLast: Option[Boolean],
                        multiValued: Option[Boolean],
                        uninvertible: Option[Boolean],
                        omitNorms: Option[Boolean],
                        omitTermFreqAndPositions: Option[Boolean],
                        omitPositions: Option[Boolean],
                        termVectors: Option[Boolean],
                        termPositions: Option[Boolean],
                        termOffsets: Option[Boolean],
                        termPayloads: Option[Boolean],
                        required: Option[Boolean],
                        useDocValuesAsStored: Option[Boolean],
                        large: Option[Boolean]) extends SchemaInstruction {
  override def json: (String, Json) = "replace-field" -> JsonUtil.toJson(this)
}
