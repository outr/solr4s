package com.outr.solr4s

import com.outr.solr4s.admin.{FacetBucket, QueryResponse, Stats}

import scala.concurrent.Future
import scribe.Execution.global

class QueryResults[I](builder: QueryBuilder[I],
                      response: QueryResponse) {
  def offset: Int = response.response.start
  def limit: Int = builder.query.limit
  def total: Int = response.response.numFound
  def maxScore: Double = response.response.maxScore
  def statsOption: Option[Stats] = response.stats
  def stats: Stats = statsOption.getOrElse(throw new RuntimeException("No stats set!"))

  lazy val docs: List[QueryResult[I]] = response.response.docs.map { json =>
    val doc = builder.converter(json)
    val id = (json \\ "id").headOption.flatMap(_.asString).getOrElse("")
    val version = (json \\ "_version_").headOption.flatMap(_.asNumber.flatMap(_.toLong)).getOrElse(0L)
    val score = (json \\ "score").headOption.flatMap(_.asNumber.map(_.toDouble)).getOrElse(0.0)
    QueryResult[I](doc, id, version, score)
  }

  lazy val count: Int = docs.size

  lazy val map: Map[I, QueryResult[I]] = docs.map(r => r.entry -> r).toMap
  lazy val entries: List[I] = docs.map(_.entry)

  def id(i: I): String = map(i).id
  def version(i: I): Long = map(i).version
  def score(i: I): Double = map(i).score

  def facet(name: String): List[FacetBucket] = response.facet(name)
  def facet[T](field: Field[T]): List[FacetBucket] = facet(field.name)

  def hasPrevious: Boolean = offset > 0
  def hasNext: Boolean = offset + limit < total

  def previous(): Future[QueryResults[I]] = {
    assert(hasPrevious, "No previous page")
    builder.offset(math.max(offset - limit, 0)).execute()
  }

  def next(): Future[QueryResults[I]] = {
    assert(hasNext, "No next page")
    builder.offset(offset + limit).execute()
  }
}