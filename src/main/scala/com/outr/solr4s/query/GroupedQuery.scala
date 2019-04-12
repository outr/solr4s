package com.outr.solr4s.query

case class GroupedQuery(condition: Condition, queries: Query*) extends Query {
  assert(queries.length > 1, "A grouped query must have at least two entries")

  override def asString: String = queries.map(q => s"(${q.asString})").mkString(s" ${condition.value} ")
}
