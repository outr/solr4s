package com.outr.solr4s.admin

sealed trait Direction

object Direction {
  case object Ascending extends Direction
  case object Descending extends Direction
}