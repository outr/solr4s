package com.outr.solr4s

import io.circe.Json

trait SolrUpdateInstruction {
  def key: String
  def value: Json
}
