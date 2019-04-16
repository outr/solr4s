package com.outr.solr4s.query

sealed trait SpatialFilter

object SpatialFilter {
  /**
    * The geofilt filter allows you to retrieve results based on the geospatial distance (AKA the "great circle
    * distance") from a given point. Another way of looking at it is that it creates a circular shape filter
    *
    * @see https://lucene.apache.org/solr/guide/7_7/spatial-search.html#geofilt
    */
  case object GeoFilter extends SpatialFilter

  /**
    * The bbox filter is very similar to geofilt except it uses the bounding box of the calculated circle.
    *
    * @see https://lucene.apache.org/solr/guide/7_7/spatial-search.html#bbox
    */
  case object BoundingBox extends SpatialFilter
}