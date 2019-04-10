package com.outr.solr4s.admin

import io.circe.Json

trait SchemaInstruction {
  def json: (String, Json)
}
