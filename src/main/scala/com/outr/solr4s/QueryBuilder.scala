package com.outr.solr4s

import com.outr.solr4s.admin.{SolrQuery, Sort}
import com.outr.solr4s.query.Query
import io.circe.{Decoder, Json}

import scala.concurrent.{ExecutionContext, Future}

case class QueryBuilder[I](converter: Json => I, query: SolrQuery) {
  def apply(query: Query): QueryBuilder[I] = copy(query = this.query(query))
  def filter(filters: Query*): QueryBuilder[I] = copy(query = this.query.filter(filters: _*))
  def offset: Int = query.offset
  def offset(offset: Int): QueryBuilder[I] = copy(query = this.query.offset(offset))
  def limit: Int = query.limit
  def limit(limit: Int): QueryBuilder[I] = copy(query = this.query.limit(limit))
  def fields(fields: String*): QueryBuilder[I] = copy(query = this.query.fields(fields: _*))
  def defType(defType: String): QueryBuilder[I] = copy(query = this.query.defType(defType))
  def sort(sort: Sort*): QueryBuilder[I] = copy(query = this.query.sort(sort: _*))
  def params(params: (String, String)*): QueryBuilder[I] = copy(query = this.query.params(params: _*))
  def as[T](converter: Json => T): QueryBuilder[T] = copy[T](converter = converter)
  def as[T](implicit decoder: Decoder[T]): QueryBuilder[T] = copy[T](converter = json => {
    decoder.decodeJson(json) match {
      case Left(df) => throw new RuntimeException(s"Failed to decode: $json", df)
      case Right(t) => t
    }
  })
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
