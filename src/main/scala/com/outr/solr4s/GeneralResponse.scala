package com.outr.solr4s

case class GeneralResponse(responseHeader: Option[ResponseHeader],
                           status: Option[ResponseStatus],
                           success: Map[String, ResponseSuccess] = Map.empty,
                           warning: Option[String],
                           exception: Option[ResponseException],
                           error: Option[ResponseError]) {
  def isSuccess: Boolean = exception.isEmpty && error.isEmpty
}
