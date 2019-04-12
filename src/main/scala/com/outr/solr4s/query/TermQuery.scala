package com.outr.solr4s.query

import perfolation._

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
