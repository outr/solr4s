package com.outr.solr4s

sealed trait Direction

object Direction {
  case object Ascending extends Direction
  case object Descending extends Direction
}