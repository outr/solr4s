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

case class QueryRequest(query: Query = MatchAllQuery,
                        filters: List[Query] = Nil,
                        offset: Int = 0,
                        limit: Int = 100,
                        fields: List[String] = List("*", "score"),
                        defType: Option[String] = None,
                        sort: List[Sort] = Nil,
                        params: Map[String, String] = Map.empty) {
  def toJSON: Json = {
    var json = Json.obj(
      "query" -> Json.fromString(query.asString),
      "offset" -> Json.fromInt(offset),
      "limit" -> Json.fromInt(limit)
    )
    def merge(entries: (String, Json)*): Unit = json = json.deepMerge(Json.obj(entries: _*))
    if (filters.nonEmpty) {
      merge("filter" -> Json.arr(filters.map(f => Json.fromString(f.asString)): _*))
    }
    if (fields.nonEmpty) {
      merge("fields" -> Json.arr(fields.map(Json.fromString): _*))
    }
    defType.foreach { t =>
      merge("defType" -> Json.fromString(t))
    }
    if (sort.nonEmpty) {
      val sortStrings = sort.map { s =>
        val direction = s.direction match {
          case Direction.Ascending => "asc"
          case Direction.Descending => "desc"
        }
        s"${s.field} $direction"
      }
      merge("sort" -> Json.fromString(sortStrings.mkString(", ")))
    }
    if (params.nonEmpty) {
      val pairs = params.toList.map {
        case (key, value) => key -> Json.fromString(value)
      }
      merge("params" -> Json.obj(pairs: _*))
    }
    json
  }
}

object QueryRequest {
  implicit val encoder: Encoder[QueryRequest] = new Encoder[QueryRequest] {
    override def apply(r: QueryRequest): Json = r.toJSON
  }
}