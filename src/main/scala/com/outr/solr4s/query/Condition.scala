package com.outr.solr4s.query

sealed trait Condition {
  def value: String
}

object Condition {
  case object And extends Condition {
    override def value: String = "AND"
  }
  case object Or extends Condition {
    override def value: String = "OR"
  }
  case object Not extends Condition {
    override def value: String = "NOT"
  }
}