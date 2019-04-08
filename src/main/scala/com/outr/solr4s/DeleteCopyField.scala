package com.outr.solr4s

import io.circe.Json
import profig.JsonUtil

case class DeleteCopyField(source: String, dest: String) extends SchemaInstruction {
  override def json: (String, Json) = "delete-copy-field" -> JsonUtil.toJson(this)
}
