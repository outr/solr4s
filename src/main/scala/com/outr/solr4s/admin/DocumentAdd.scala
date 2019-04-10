package com.outr.solr4s.admin

import io.circe.Json
import profig.JsonUtil

case class DocumentAdd(doc: Json,
                       commitWithin: Option[Long] = None,
                       overwrite: Option[Boolean] = None) extends SolrUpdateInstruction {
  override def key: String = "add"
  override def value: Json = JsonUtil.toJson(this)
}
