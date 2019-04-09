package com.outr.solr4s

import com.outr.solr4s.query.{MatchAllQuery, Query}
import io.youi.net.Path

import scala.concurrent.{ExecutionContext, Future}

case class SolrQuery(query: Query = MatchAllQuery, limit: Int = 10, filters: List[String] = Nil, collection: SolrCollection) {
  def apply(query: Query): SolrQuery = copy(query = query)
  def filter(filters: String*): SolrQuery = copy(filters = this.filters ::: filters.toList)

  def execute()(implicit ec: ExecutionContext): Future[QueryResponse] = collection
    .api
    .client
    .path(Path.parse(s"/solr/${collection.collectionName}/query"))
    .restful[QueryRequest, QueryResponse](QueryRequest(
      query = query.asString,
      limit = limit,
      filter = filters
    ))

  case class QueryRequest(query: String, limit: Int, filter: List[String])
}