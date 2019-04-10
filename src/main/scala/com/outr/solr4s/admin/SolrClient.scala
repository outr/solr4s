package com.outr.solr4s.admin

import io.youi.net._

case class SolrClient(url: URL = url"http://localhost:8983") {
  val api: SolrAPI = new SolrAPI(this)
}