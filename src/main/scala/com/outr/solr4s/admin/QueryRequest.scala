package com.outr.solr4s.admin

import com.outr.solr4s.query.{MatchAllQuery, Query}
import io.circe.{Encoder, Json}
import io.youi.net.Path
import profig.JsonUtil

import scala.concurrent.{ExecutionContext, Future}

case class QueryRequest(collection: SolrCollection,
                        query: Query = MatchAllQuery,
                        filters: List[Query] = Nil,
                        offset: Int = 0,
                        limit: Int = 100,
                        fields: List[String] = List("*", "score"),
                        defType: Option[String] = None,
                        sort: List[Sort] = Nil,
                        params: Map[String, String] = Map.empty,
                        facets: Map[String, FacetQuery] = Map.empty) {
  def execute()(implicit ec: ExecutionContext): Future[QueryResponse] = {
    var url = collection.api.client.url
    if (sort.nonEmpty) {
      val sortStrings = sort.map { s =>
        val direction = s.direction match {
          case Direction.Ascending => "asc"
          case Direction.Descending => "desc"
        }
        s"${s.field} $direction"
      }
      url = url.withParam("sort", sortStrings.mkString(","))
    }
    collection
      .api
      .client
      .url(url)
      .path(Path.parse(s"/solr/${collection.collectionName}/query"))
      .restful[QueryRequest, QueryResponse](this)
  }

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
    if (params.nonEmpty) {
      val pairs = params.toList.map {
        case (key, value) => key -> Json.fromString(value)
      }
      merge("params" -> Json.obj(pairs: _*))
    }
    if (facets.nonEmpty) {
      val entries = facets.toList.map(t => t._1 -> JsonUtil.toJson(t._2))
      merge("facet" -> Json.obj(entries: _*))
    }
    json
  }
}

object QueryRequest {
  implicit val encoder: Encoder[QueryRequest] = new Encoder[QueryRequest] {
    override def apply(r: QueryRequest): Json = r.toJSON
  }
}