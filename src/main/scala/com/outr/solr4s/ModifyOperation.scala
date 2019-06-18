package com.outr.solr4s

import io.circe.Json

case class ModifyOperation(field: String, operation: String, value: Json) {
  lazy val json: Json = Json.obj(field -> Json.obj(operation -> value))
}
