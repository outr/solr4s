package com.outr.solr4s

import com.outr.solr4s.query.{MatchAllQuery, Query}
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

  // TODO: support sort, facet, and params

  def execute()(implicit ec: ExecutionContext): Future[QueryResponse] = collection
    .api
    .client
    .path(Path.parse(s"/solr/${collection.collectionName}/query"))
    .restful[QueryRequest, QueryResponse](request)
}

case class QueryRequest(query: Query = MatchAllQuery,
                        filters: List[Query] = Nil,
                        offset: Int = 0,
                        limit: Int = 100,
                        fields: List[String] = Nil) {
  def toJSON: Json = {
    var json = Json.obj(
      "query" -> Json.fromString(query.asString),
      "offset" -> Json.fromInt(offset),
      "limit" -> Json.fromInt(limit)
    )
    if (filters.nonEmpty) {
      json = json.deepMerge(Json.obj("filter" -> Json.arr(filters.map(f => Json.fromString(f.asString)): _*)))
    }
    if (fields.nonEmpty) {
      json = json.deepMerge(Json.obj("fields" -> Json.arr(fields.map(Json.fromString): _*)))
    }
    json
  }
}

object QueryRequest {
  implicit val encoder: Encoder[QueryRequest] = new Encoder[QueryRequest] {
    override def apply(r: QueryRequest): Json = r.toJSON
  }
}