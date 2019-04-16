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
        .city
        .add(
          City("New York City", newYorkCity),
          City("Chicago", chicago),
          City("Jefferson Valley", jeffersonValley),
          City("Noble", noble),
          City("Oklahoma City", oklahomaCity),
          City("Yonkers", yonkers)
        )
        .commit()
        .execute()
        .map { response =>
          response.isSuccess should be(true)
        }
    }
    "query back all cities" in {
      Indexed
        .city
        .query
        .sort(Indexed.city.name.ascending)
        .execute()
        .map { results =>
          results.total should be(6)
          results.docs.map(_.doc.name) should be(List("Chicago", "New York City", "Oklahoma City", "Jefferson Valley", "Noble", "Yonkers"))
        }
    }
    "query back all cities sorted by distance to Oklahoma City" in {
      Indexed
        .city
        .query
        .sort(oklahomaCity.sort("location", Direction.Ascending))
        .execute()
        .map { results =>
          results.total should be(6)
          results.docs.map(_.doc.name) should be(List("Oklahoma City", "Noble", "Chicago", "New York City", "Yonkers", "Jefferson Valley"))
        }
    }
    "filter results" in {
      Indexed
        .city
        .query(oklahomaCity.filter("location", 5L))
        .execute()
        .map { results =>
          results.total should be(2)
        }
    }
    "filter and sort results" in {
      Indexed
        .city
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
        .city
        .query.filter(
          and(
            oklahomaCity.filter("location", 5L),
            noble.filter("location", 5L),
            Indexed.city.name === "Noble"
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

  case class City(name: String, location: SpatialPoint)

  class CityIndex(override val solr: SolrIndexed) extends IndexedCollection[City] {
    val name: Field[String] = Field[String]("name", FieldType.TextEnglish)
    val location: Field[SpatialPoint] = Field[SpatialPoint]("location", FieldType.Point)

    override def fields: List[Field[_]] = List(name, location)

    override def toJSON(i: City): Json = JsonUtil.toJson[City](i)
    override def fromJSON(json: Json): City = JsonUtil.fromJson[City](json)
  }

  object Indexed extends SolrIndexed {
    val city: CityIndex = new CityIndex(this)
  }
}
