package com.outr.solr4s.query

object MatchAllQuery extends Query {
  override def asString: String = "*:*"
}