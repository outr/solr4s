package com.outr.solr4s.admin.query

import perfolation._

sealed trait Query {
  def asString: String
}

object MatchAllQuery extends Query {
  override def asString: String = "*:*"
}

case class TermQuery(value: QueryValue,
                     field: Option[String] = None,
                     proximity: Option[Int] = None,
                     boost: Option[Double] = None,
                     constantScore: Option[Double] = None,
                     operator: Operator = Operator.Should) extends Query {
  lazy val isPhrase: Boolean = value.value.contains(' ')

  override def asString: String = {
    val b = new StringBuilder
    b.append(operator.prefix)
    field.foreach { f =>
      b.append(f)
      b.append(':')
    }
    if (isPhrase) b.append('"')
    b.append(value.value)
    if (isPhrase) b.append('"')
    proximity.foreach { p =>
      b.append('~')
      b.append(p)
    }
    boost.foreach { i =>
      b.append('^')
      b.append(i.f(f = 0, maxF = 4))
    }
    constantScore.foreach { s =>
      b.append("^=")
      b.append(s.f(f = 0, maxF = 4))
    }
    b.toString()
  }
}

case class GroupedQuery(condition: Condition, queries: Query*) extends Query {
  assert(queries.length > 1, "A grouped query must have at least two entries")

  override def asString: String = queries.map(q => s"(${q.asString})").mkString(s" ${condition.value} ")
}

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