package com.outr.solr4s

import io.circe.Json

trait SchemaInstruction {
  def json: (String, Json)
}
