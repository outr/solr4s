package com.outr.solr4s.admin

import com.outr.solr4s.admin.query.{MatchAllQuery, Query}
import io.circe.{Encoder, Json}
import io.youi.net.Path

import scala.concurrent.{ExecutionContext, Future}

case class SolrQuery(collection: SolrCollection, request: QueryRequest = QueryRequest()) {
  def modify(f: QueryRequest => QueryRequest): SolrQuery = copy(request = f(request))

  def apply(query: Query): SolrQuery = modify(_.copy(query = query))
  def filter(filters: Query*): SolrQuery = modify(_.copy(filters = request.filters ::: filters.toList))
  def offset(offset: Int): SolrQuery = modify(_.copy(offset = offset))
  def limit(limit: Int): SolrQuery = modify(_.copy(limit = limit))
  def fields(fields: String*): SolrQuery = modify(_.copy(fields = fields.toList))
  def defType(defType: String): SolrQuery = modify(_.copy(defType = Some(defType)))
  def sort(sort: Sort*): SolrQuery = modify(_.copy(sort = sort.toList))
  def params(params: (String, String)*): SolrQuery = modify(_.copy(params = request.params ++ params.toMap))

  // TODO: support facet

  def execute()(implicit ec: ExecutionContext): Future[QueryResponse] = collection
    .api
    .client
    .path(Path.parse(s"/solr/${collection.collectionName}/query"))
    .restful[QueryRequest, QueryResponse](request)
}