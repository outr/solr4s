package spec

import com.outr.solr4s.admin.Direction
import com.outr.solr4s.query._
import com.outr.solr4s.{Field, FieldType, IndexedCollection, SolrIndexed, SpatialPoint}
import io.circe.Json
import org.scalatest.{AsyncWordSpec, Matchers}
import profig.JsonUtil

class SpatialSpec extends AsyncWordSpec with Matchers {
  "Spatial spec" should {
    val newYorkCity = SpatialPoint(40.7142, -74.0119)
    val chicago = SpatialPoint(41.8119,	-87.6873)
    val jeffersonValley = SpatialPoint(41.3385, -73.7947)
    val noble = SpatialPoint(35.1417, -97.3409)
    val oklahomaCity = SpatialPoint(35.5514, -97.4075)
    val yonkers = SpatialPoint(40.9461, -73.8669)

    "create the collection" in {
      Indexed.create().map { _ =>
        succeed
      }
    }
    "insert a few cities" in {
      Indexed
        .location
        .add(
          Location("New York City", List(newYorkCity)),
          Location("Chicago", List(chicago)),
          Location("Jefferson Valley", List(jeffersonValley)),
          Location("Noble", List(noble)),
          Location("Oklahoma City", List(oklahomaCity)),
          Location("Yonkers", List(yonkers)),
          Location("Oklahoma", List(noble, oklahomaCity))
        )
        .commit()
        .execute()
        .map { response =>
          response.isSuccess should be(true)
        }
    }
    "query back all cities" in {
      Indexed
        .location
        .query
        .sort(Indexed.location.name.ascending)
        .execute()
        .map { results =>
          results.total should be(7)
          results.docs.map(_.doc.name) should be(List("Chicago", "New York City", "Oklahoma City", "Jefferson Valley", "Noble", "Oklahoma", "Yonkers"))
        }
    }
    "query back all cities sorted by distance to Oklahoma City" in {
      Indexed
        .location
        .query
        .sort(oklahomaCity.sort("location", Direction.Ascending))
        .execute()
        .map { results =>
          results.total should be(7)
          results.docs.map(_.doc.name) should be(List("Oklahoma City", "Oklahoma", "Noble", "Chicago", "New York City", "Yonkers", "Jefferson Valley"))
        }
    }
    "filter results" in {
      Indexed
        .location
        .query(oklahomaCity.filter("location", 5L))
        .execute()
        .map { results =>
          results.total should be(2)
        }
    }
    "filter and sort results" in {
      Indexed
        .location
        .query(oklahomaCity.filter("location", 5L))
        .sort(oklahomaCity.sort("location", Direction.Ascending))
        .execute()
        .map { results =>
          results.total should be(2)
          results.docs.head.doc.name should be("Oklahoma City")
        }
    }
    "complex filtering" in {
      Indexed
        .location
        .query.filter(
          and(
            oklahomaCity.filter("location", 50L),
            noble.filter("location", 50L),
            Indexed.location.name === "Noble"
          )
        )
        .execute()
        .map { results =>
          results.docs.map(_.doc.name) should be(List("Noble"))
        }
    }
    "delete the collection" in {
      Indexed.delete().map { _ =>
        succeed
      }
    }
    "verify the collection no longer exists" in {
      Indexed.client.api.collections.list().map { list =>
        list.collections should not contain "city"
      }
    }
  }

  case class Location(name: String, location: List[SpatialPoint])

  class LocationIndex(override val solr: SolrIndexed) extends IndexedCollection[Location] {
    val name: Field[String] = Field[String]("name", FieldType.TextEnglish)
    val location: Field[SpatialPoint] = Field[SpatialPoint]("location", FieldType.Location, multiValued = true)

    override def fields: List[Field[_]] = List(name, location)

    override def toJSON(i: Location): Json = JsonUtil.toJson[Location](i)
    override def fromJSON(json: Json): Location = JsonUtil.fromJson[Location](json)
  }

  object Indexed extends SolrIndexed {
    val location: LocationIndex = new LocationIndex(this)
  }
}
