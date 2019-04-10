package com.outr.solr4s.admin

import io.circe.Json

trait SolrUpdateInstruction {
  def key: String
  def value: Json
}
