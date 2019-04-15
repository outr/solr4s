package com.outr.solr4s.admin

import io.circe.Json
import profig.JsonUtil

case class QueryResponse(responseHeader: ResponseHeader,
                         response: QueryResponseData = QueryResponseData(0, 0, 0.0, Nil),
                         facets: Option[Json],
                         error: Option[ResponseError]) {
  def facet(name: String): List[FacetBucket] = {
    val json = (facets.getOrElse(throw new RuntimeException("No facets in response")) \\ name).head
    val buckets = (json \\ "buckets").head
    JsonUtil.fromJson[List[FacetBucket]](buckets)
  }
}

case class FacetBucket(`val`: String, count: Int)