package com.outr.solr4s.query

case class GroupedQuery(condition: Condition, queries: List[Query]) extends Query {
  assert(queries.length > 1, "A grouped query must have at least two entries")

  def append(queries: Query*): GroupedQuery = copy(condition, this.queries ::: queries.toList)

  override def asString: String = queries.map(q => s"(${q.asString})").mkString(s" ${condition.value} ")
}