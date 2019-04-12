package com.outr.solr4s.query

sealed trait Operator {
  def prefix: String
}

object Operator {
  case object Must extends Operator {
    override def prefix: String = "+"
  }
  case object Should extends Operator {
    override def prefix: String = ""
  }
  case object MustNot extends Operator {
    override def prefix: String = "-"
  }
}