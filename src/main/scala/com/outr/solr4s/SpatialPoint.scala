package com.outr.solr4s

import com.outr.solr4s.admin.{Direction, Sort}
import io.circe.Decoder.Result
import io.circe.{Decoder, Encoder, HCursor, Json}

case class SpatialPoint(latitude: Double, longitude: Double) {
  def sort(field: String, direction: Direction): Sort = Sort(
    field = s"{!sfield=$field pt=$latitude,$longitude}geodist()",
    direction = direction
  )
}

object SpatialPoint {
  implicit val encoder: Encoder[SpatialPoint] = new Encoder[SpatialPoint] {
    override def apply(point: SpatialPoint): Json = Json.fromString(s"${point.latitude},${point.longitude}")
  }

  implicit val decoder: Decoder[SpatialPoint] = new Decoder[SpatialPoint] {
    override def apply(c: HCursor): Result[SpatialPoint] = {
      Decoder.decodeString(c).map { s =>
        val comma = s.indexOf(',')
        SpatialPoint(
          latitude = s.substring(0, comma).toDouble,
          longitude = s.substring(comma + 1).toDouble
        )
      }
    }
  }
}