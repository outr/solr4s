package com.outr.solr4s

import io.youi.net.Path

import scala.concurrent.{ExecutionContext, Future}

case class SolrQuery(query: String = "*:*", filter: Option[String] = None, collection: SolrCollection) {
  def apply(query: String): SolrQuery = copy(query = query)
  def filter(filter: String): SolrQuery = copy(filter = Some(filter))

  def execute()(implicit ec: ExecutionContext): Future[QueryResponse] = collection
    .api
    .client
    .path(Path.parse(s"/solr/${collection.collectionName}/query"))
    .param("q", query, "")
    .call[QueryResponse]
}
