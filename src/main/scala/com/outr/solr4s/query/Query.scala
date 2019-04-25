package com.outr.solr4s.query

trait Query {
  def asString: String

  def and(that: Query): Query = this match {
    case t: GroupedQuery if t.condition == Condition.And => t.append(that)
    case _ => GroupedQuery(Condition.And, List(this, that))
  }

  def or(that: Query): Query = this match {
    case t: GroupedQuery if t.condition == Condition.Or => t.append(that)
    case _ => GroupedQuery(Condition.Or, List(this, that))
  }

  def not(that: Query): Query = this match {
    case t: GroupedQuery if t.condition == Condition.Not => t.append(that)
    case _ => GroupedQuery(Condition.Not, List(this, that))
  }
}

object Query {
  def all: Query = MatchAllQuery
}