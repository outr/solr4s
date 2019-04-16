package com.outr.solr4s.query

import com.outr.solr4s.SpatialPoint

case class SpatialQuery(filter: SpatialFilter, field: String, point: SpatialPoint, distance: Long) extends Query {
  override def asString: String = {
    val f = filter match {
      case SpatialFilter.GeoFilter => "geofilt"
      case SpatialFilter.BoundingBox => "bbox"
    }
    s"{!$f sfield=$field pt=${point.latitude},${point.longitude} d=$distance}"
  }
}
