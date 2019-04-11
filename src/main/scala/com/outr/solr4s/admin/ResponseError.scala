package com.outr.solr4s.admin

import io.circe.Json

case class ResponseError(metadata: List[String], details: List[Json] = Nil, msg: String, code: Int) {
  override def toString: String = s"$msg ($code): $details [$metadata]"
}