package com.outr.solr4s.admin

import com.outr.solr4s.SolrException

case class GeneralResponse(responseHeader: Option[ResponseHeader],
                           status: Option[ResponseStatus],
                           success: Map[String, ResponseSuccess] = Map.empty,
                           warning: Option[String],
                           exception: Option[ResponseException],
                           error: Option[ResponseError]) {
  def isSuccess: Boolean = exception.isEmpty && error.isEmpty
  def successOrException(): Boolean = if (!isSuccess) {
    throw SolrException(this)
  } else {
    true
  }
  def message: String = error.map(_.msg).orElse(exception.map(_.msg)).orElse(warning).getOrElse("No message")
}