package com.outr.solr4s.admin

import io.youi.net._

import scala.concurrent.{ExecutionContext, Future}

class SolrCollections(api: SolrAPI) {
  def list()(implicit ec: ExecutionContext): Future[CollectionsList] = api
    .client
    .path(path"/solr/admin/collections")
    .params("action" -> "LIST", "wt" -> "json")
    .call[CollectionsList]
}
