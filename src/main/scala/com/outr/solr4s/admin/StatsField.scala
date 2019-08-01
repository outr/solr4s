package com.outr.solr4s.admin

case class StatsField[T](min: T,
                         max: T,
                         count: Int,
                         missing: Int,
                         distinctValues: List[T] = Nil,
                         countDistinct: Option[Int],
                         sum: Option[Double],
                         sumOfSquares: Option[Double],
                         mean: Option[Double],
                         stddev: Option[Double])
