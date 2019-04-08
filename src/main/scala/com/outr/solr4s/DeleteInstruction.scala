package com.outr.solr4s

import io.circe.Json
import profig.JsonUtil

case class DeleteInstruction(id: Option[String], query: Option[String]) extends SolrUpdateInstruction {
  override def key: String = "delete"
  override def value: Json = JsonUtil.toJson(this)
}
