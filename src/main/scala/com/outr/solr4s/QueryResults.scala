package com.outr.solr4s

import com.outr.solr4s.admin.{FacetBucket, QueryResponse}

class QueryResults[I](builder: QueryBuilder[I],
                      response: QueryResponse) {
  def offset: Int = response.response.start
  def limit: Int = builder.query.limit
  def total: Int = response.response.numFound
  def maxScore: Double = response.response.maxScore

  lazy val docs: List[QueryResult[I]] = response.response.docs.map { json =>
    val doc = builder.converter(json)
    val id = (json \\ "id").headOption.flatMap(_.asString).getOrElse("")
    val version = (json \\ "_version_").headOption.flatMap(_.asNumber.flatMap(_.toLong)).getOrElse(0L)
    val score = (json \\ "score").headOption.flatMap(_.asNumber.map(_.toDouble)).getOrElse(0.0)
    QueryResult[I](doc, id, version, score)
  }

  def facet(name: String): List[FacetBucket] = response.facet(name)
  def facet[T](field: Field[T]): List[FacetBucket] = facet(field.name)
}