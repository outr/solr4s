package com.outr.solr4s

import io.circe.Json
import profig.JsonUtil

case class DeleteField(name: String) extends SchemaInstruction {
  override def json: (String, Json) = "delete-field" -> JsonUtil.toJson(this)
}
