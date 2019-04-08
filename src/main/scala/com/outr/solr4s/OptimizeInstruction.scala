package com.outr.solr4s

import io.circe.Json
import profig.JsonUtil

case class OptimizeInstruction(waitSearcher: Boolean) extends SolrUpdateInstruction {
  override def key: String = "optimize"
  override def value: Json = JsonUtil.toJson(this)
}
