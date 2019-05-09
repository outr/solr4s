package com.outr.solr4s.name

sealed trait NamePartType

object NamePartType {
  case object Salutation extends NamePartType
  case object Prefix extends NamePartType
  case object Primary extends NamePartType
  case object Nickname extends NamePartType
  case object Family extends NamePartType
  case object Suffix extends NamePartType
  case object Postnominal extends NamePartType
  case object Comma extends NamePartType

  lazy val all: Set[NamePartType] = Set(Salutation, Prefix, Primary, Nickname, Family, Suffix, Postnominal, Comma)
}