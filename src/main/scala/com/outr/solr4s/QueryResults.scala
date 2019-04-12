package com.outr.solr4s

case class QueryResults[I](docs: List[QueryResult[I]], offset: Int, limit: Int, total: Int, maxScore: Double)
