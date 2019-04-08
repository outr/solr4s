package com.outr.solr4s

import io.circe.Json

case class QueryResponseData(numFound: Int, start: Int, maxScore: Double, docs: List[Json])
