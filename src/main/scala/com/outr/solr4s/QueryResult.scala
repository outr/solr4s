package com.outr.solr4s

case class QueryResult[I](entry: I, id: String, version: Long, score: Double)