package spec

import com.outr.solr4s.admin.FacetBucket
import com.outr.solr4s.{Field, IndexedCollection, SolrIndexed, FieldType}
import com.outr.solr4s.query._
import io.circe.Json
import io.circe.generic.auto._
import org.scalatest.{AsyncWordSpec, Matchers}
import profig.JsonUtil

class SimpleSpec extends AsyncWordSpec with Matchers {
  "Simple spec" should {
    val newYorkCity = "New York City"
    val chicago = "Chicago"
    val jeffersonValley = "Jefferson Valley"
    val noble = "Noble"
    val oklahomaCity = "Oklahoma City"
    val yonkers = "Yonkers"

    val adam = Person("Adam", "adam@solr4s", 21, 1.23, 4321L, List(newYorkCity, yonkers), enabled = true)
    val bethany = Person("Bethany", "bethany@solr4s", 22, 1.24, 54321L, Nil, enabled = false)
    val charlie = Person("Charlie", "charlie@solr4s", 20, 1.25, 34321L, List(chicago, jeffersonValley), enabled = true)
    val debbie = Person("Debbie", "debbie@solr4s", 19, 1.26, 64321L, List(noble, oklahomaCity, newYorkCity), enabled = false)

    "verify the collections" in {
      Indexed.collections.map(_.collectionName) should be(List("person"))
    }
    "create the collection" in {
      Indexed.person.collectionName should be("person")
      Indexed.create().map { _ =>
        succeed
      }
    }
    "insert a simple document" in {
      Indexed.person.add(adam).commit().execute().map { r =>
        r.isSuccess should be(true)
      }
    }
    "query back the simple document" in {
      Indexed.person.query.execute().map { results =>
        results.total should be(1)
        results.docs.map(_.doc) should be(List(adam))
        results.maxScore should be(1.0)
        results.docs.head.score should be(1.0)
        results.docs.head.id.length should be(36)
        results.docs.head.version should be > 0L
      }
    }
    "insert several documents" in {
      Indexed
        .person
        .add(bethany, charlie, debbie)
        .commit()
        .execute()
        .map { r =>
          r.isSuccess should be(true)
        }
    }
    "query back all the documents" in {
      Indexed.person.query.execute().map { results =>
        results.total should be(4)
        results.docs.map(_.doc).toSet should be(Set(adam, bethany, charlie, debbie))
        results.maxScore should be(1.0)
      }
    }
    "query back bethany by name" in {
      Indexed
        .person
        .query(Indexed.person.name === "bethany")
        .execute()
        .map { results =>
          results.total should be(1)
          results.docs.map(_.doc) should be(List(bethany))
          results.docs.head.id.length should be(36)
          results.docs.head.version should be > 0L
        }
    }
    "query back by age" in {
      Indexed
        .person
        .query(Indexed.person.age === 21)
        .execute()
        .map { results =>
          results.docs.map(_.doc.name) should be(List("Adam"))
        }
    }
    "query back sorting by age" in {
      Indexed
        .person
        .query
        .sort(Indexed.person.age.ascending)
        .execute()
        .map { results =>
          results.total should be(4)
          results.docs.map(_.doc.name).toSet should be(Set("Debbie", "Charlie", "Adam", "Bethany"))
        }
    }
    "query back by city" in {
      Indexed
        .person
        .query
        .filter(Indexed.person.cities === newYorkCity)
        .sort(Indexed.person.name.ascending)
        .execute()
        .map { results =>
          results.docs.map(_.doc.name) should be(List("Adam", "Debbie"))
        }
    }
    "query back by exact cities" in {
      Indexed
        .person
        .query
        .filter((Indexed.person.cities === chicago) and (Indexed.person.cities === jeffersonValley))
        .sort(Indexed.person.name.ascending)
        .execute()
        .map { results =>
          results.docs.map(_.doc.name) should be(List("Charlie"))
        }
    }
    "query back 'enabled' facets" in {
      Indexed
        .person
        .query
        .facet(Indexed.person.enabled)
        .execute()
        .map { results =>
          results.total should be(4)
          val buckets = results.facet(Indexed.person.enabled)
          buckets.find(_.`val` == "true") should be(Some(FacetBucket("true", 2)))
          buckets.find(_.`val` == "false") should be(Some(FacetBucket("false", 2)))
        }
    }
    "query back and convert to SimplePerson" in {
      Indexed
        .person
        .query
        .sort(Indexed.person.name.ascending)
        .fields(Indexed.person.name)
        .as[SimplePerson]
        .execute()
        .map { results =>
          results.total should be(4)
          results.docs.map(_.doc) should be(List(
            SimplePerson("Adam"),
            SimplePerson("Bethany"),
            SimplePerson("Charlie"),
            SimplePerson("Debbie"),
          ))
        }
    }
    "verify the collection exists" in {
      Indexed.client.api.collections.list().map { list =>
        list.collections should contain("person")
      }
    }
    "delete the collection" in {
      Indexed.delete().map { _ =>
        succeed
      }
    }
    "verify the collection no longer exists" in {
      Indexed.client.api.collections.list().map { list =>
        list.collections should not contain "person"
      }
    }
  }

  // TODO: SBT plugin like giant-scala?

  case class Person(name: String,
                    email: String,
                    age: Int,
                    progress: Double,
                    bytes: Long,
                    cities: List[String] = Nil,
                    enabled: Boolean)

  case class SimplePerson(name: String)

  class PersonIndex(override val solr: SolrIndexed) extends IndexedCollection[Person] {
    val name: Field[String] = Field[String]("name", FieldType.TextEnglish)
    val email: Field[String] = Field[String]("email", FieldType.TextEnglish)
    val age: Field[Int] = Field[Int]("age", FieldType.IntPoint)
    val progress: Field[Double] = Field[Double]("progress", FieldType.DoublePoint)
    val bytes: Field[Long] = Field[Long]("bytes", FieldType.LongPoint)
    val cities: Field[List[String]] = Field[List[String]]("cities", FieldType.TextEnglish, multiValued = true)
    val enabled: Field[Boolean] = Field[Boolean]("enabled", FieldType.Boolean)

    override def toJSON(i: Person): Json = JsonUtil.toJson[Person](i)
    override def fromJSON(json: Json): Person = JsonUtil.fromJson[Person](json)

    override def fields: List[Field[_]] = List(name, age, progress, bytes, enabled, email)
  }

  object Indexed extends SolrIndexed() {
    val person: PersonIndex = new PersonIndex(this)
  }
}