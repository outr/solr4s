package com.outr.solr4s

import io.circe.Json

case class ResponseError(metadata: List[String], details: List[Json] = Nil, msg: String, code: Int)
