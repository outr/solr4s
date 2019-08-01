package com.outr.solr4s.admin

import io.circe.Json

case class QueryResponse(responseHeader: ResponseHeader,
                         response: QueryResponseData = QueryResponseData(0, 0, 0.0, Nil),
                         facets: Option[Json],
                         stats: Option[Stats],
                         error: Option[ResponseError]) {
  def facet(name: String): List[FacetBucket] = {
    val json = (facets.getOrElse(throw new RuntimeException("No facets in response")) \\ name).head
    val buckets = (json \\ "buckets").head.asArray.map(_.toList).getOrElse(Nil)
    buckets.map { b =>
      FacetBucket(
        `val` = (b \\ "val").head match {
          case j if j.isString => j.asString.getOrElse("")
          case j => j.toString()
        },
        count = (b \\ "count").head.asNumber.flatMap(_.toInt).getOrElse(0)
      )
    }
  }
}