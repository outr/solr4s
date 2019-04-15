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
  def fields(fields: Field[_]*): QueryBuilder[I] = copy(query = this.query.fields(fields.map(_.name): _*))
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
  def facet[T](field: Field[T]): QueryBuilder[I] = copy(query = this.query.facet(field.name))
  def execute()(implicit ec: ExecutionContext): Future[QueryResults[I]] = query.execute()(ec).map { r =>
    new QueryResults[I](this, r)
  }
}
