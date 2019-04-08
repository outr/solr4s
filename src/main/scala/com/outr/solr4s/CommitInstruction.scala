package com.outr.solr4s

import io.circe.Json

object CommitInstruction extends SolrUpdateInstruction {
  override def key: String = "commit"
  override def value: Json = Json.obj()
}
