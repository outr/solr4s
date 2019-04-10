package com.outr.solr4s.admin

import io.circe.Json

case class QueryResponseData(numFound: Int, start: Int, maxScore: Double = 0.0, docs: List[Json])