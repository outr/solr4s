package com.outr.solr4s

import io.circe.Json
import profig.JsonUtil

case class AddCopyField(source: String, dest: String, maxChars: Option[Int]) extends SchemaInstruction {
  override def json: (String, Json) = "add-copy-field" -> JsonUtil.toJson(this)
}
