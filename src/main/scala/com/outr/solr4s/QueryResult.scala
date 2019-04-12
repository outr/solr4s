package com.outr.solr4s

case class QueryResult[I](doc: I, id: String, version: Long, score: Double)