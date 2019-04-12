package com.outr.solr4s

import com.outr.solr4s.admin.SolrQuery
import io.circe.Json

import scala.concurrent.{ExecutionContext, Future}

case class QueryBuilder[I](converter: Json => I, query: SolrQuery) {
  def execute()(implicit ec: ExecutionContext): Future[QueryResults[I]] = query.execute()(ec).map { r =>
    val docs = r.response.docs.map { json =>
      val doc = converter(json)
      val id = (json \\ "id").head.asString.getOrElse("")
      val version = (json \\ "_version_").head.asNumber.flatMap(_.toLong).getOrElse(0L)
      val score = (json \\ "score").head.asNumber.map(_.toDouble).getOrElse(0.0)
      QueryResult[I](doc, id, version, score)
    }
    QueryResults[I](docs, r.response.start, query.limit, r.response.numFound, r.response.maxScore)
  }
}
