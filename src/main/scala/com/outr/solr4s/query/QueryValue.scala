package com.outr.solr4s.query

sealed trait QueryValue {
  def value: String
}

object QueryValue {
  case object All extends QueryValue {
    override def value: String = "*"
  }
  case class Simple(value: String) extends QueryValue
  case class Range(from: QueryValue, to: QueryValue) extends QueryValue {
    override def value: String = s"[${from.value} TO ${to.value}]"
  }

  def apply(value: String): QueryValue = Simple(value)
  def apply(value: Int): QueryValue = Simple(value.toString)
}