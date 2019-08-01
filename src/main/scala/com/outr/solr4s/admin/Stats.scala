package com.outr.solr4s.admin

import com.outr.solr4s.{Field, Macros}
import io.circe.Json

import scala.language.experimental.macros

case class Stats(stats_fields: Map[String, Json]) {
  def apply[T](field: Field[T]): StatsField[T] = macro Macros.stats[T]
}
