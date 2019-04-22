package com.outr.solr4s.admin

import io.circe.{Json, Printer}
import io.youi.client.HttpClient
import io.youi.client.intercept.Interceptor
import io.youi.http.{HttpRequest, HttpResponse, HttpStatus}

import scala.concurrent.Future

class SolrAPI(solr: SolrClient) extends Interceptor {
  private[solr4s] lazy val client = HttpClient
    .url(solr.url)
    .noFailOnHttpStatus
    .dropNullValuesInJson(true)
    .interceptor(this)

  lazy val collections: SolrCollections = new SolrCollections(this)

  def collection(name: String): SolrCollection = new SolrCollection(name, this)

  override def before(request: HttpRequest): Future[HttpRequest] = Future.successful(request)

  override def after(request: HttpRequest, response: HttpResponse): Future[HttpResponse] = {
    if (response.status != HttpStatus.OK) {
      scribe.warn(s"[${request.url}] ${request.method}: ${request.content.map(_.asString).getOrElse("")}")
      scribe.warn(s"[${request.url.decoded}] ${response.status.message} (${response.status.code}) Received: ${response.content.map(_.asString).getOrElse("")}")
    }
    Future.successful(response)
  }
}

object SolrAPI {
  val printer: Printer = Printer.spaces2.copy(dropNullValues = true)

  def jsonObj(tuples: List[(String, Json)]): String = {
    tuples.map(t => s""" "${t._1}": ${t._2.pretty(printer)}""").mkString("{", ", ", "}")
  }
}