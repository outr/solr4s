package com.outr.solr4s.query

case class GroupedQuery(condition: Condition, queries: List[Query]) extends Query {
  assert(queries.nonEmpty, "A grouped query must have at least one entry")

  def append(queries: Query*): GroupedQuery = copy(condition, this.queries ::: queries.toList)

  override def asString: String = queries.map(q => s"(${q.asString})").mkString(s" ${condition.value} ")
}