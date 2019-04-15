package com.outr.solr4s.admin

import com.outr.solr4s.query.Query

import scala.concurrent.{ExecutionContext, Future}

case class SolrQuery(request: QueryRequest) {
  def modify(f: QueryRequest => QueryRequest): SolrQuery = copy(request = f(request))

  def apply(query: Query): SolrQuery = modify(_.copy(query = query))
  def filter(filters: Query*): SolrQuery = modify(_.copy(filters = request.filters ::: filters.toList))
  def offset: Int = request.offset
  def offset(offset: Int): SolrQuery = modify(_.copy(offset = offset))
  def limit: Int = request.limit
  def limit(limit: Int): SolrQuery = modify(_.copy(limit = limit))
  def fields(fields: String*): SolrQuery = modify(_.copy(fields = fields.toList))
  def defType(defType: String): SolrQuery = modify(_.copy(defType = Some(defType)))
  def sort(sort: Sort*): SolrQuery = modify(_.copy(sort = sort.toList))
  def params(params: (String, String)*): SolrQuery = modify(_.copy(params = request.params ++ params.toMap))
  def facet(name: String, `type`: Option[String] = None, alias: Option[String] = None): SolrQuery = {
    val f = FacetQuery(name, `type`)
    modify(_.copy(facets = request.facets + (alias.getOrElse(name) -> f)))
  }

  def execute()(implicit ec: ExecutionContext): Future[QueryResponse] = request.execute()(ec)
}