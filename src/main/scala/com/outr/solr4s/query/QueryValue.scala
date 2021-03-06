package com.outr.solr4s.query

sealed trait QueryValue {
  def value: String
}

object QueryValue {
  case object All extends QueryValue {
    override def value: String = "*"
  }
  case class Raw(value: String) extends QueryValue
  case class Range(from: QueryValue, to: QueryValue) extends QueryValue {
    override def value: String = s"[${from.value} TO ${to.value}]"
  }

  def apply(value: String): QueryValue = Raw(if (value.contains(' ')) {
    s""""$value""""
  } else {
    s"'${value.replaceAllLiterally("'", "\\'")}'"
  })
  def apply(value: Boolean): QueryValue = Raw(value.toString)
  def apply(value: Int): QueryValue = Raw(value.toString)
  def apply(value: Long): QueryValue = Raw(value.toString)
  def apply(value: Double): QueryValue = Raw(value.toString)
  def raw(value: String): QueryValue = Raw(value)
}