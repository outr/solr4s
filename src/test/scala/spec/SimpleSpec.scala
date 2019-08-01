package spec

import com.outr.solr4s.admin.FacetBucket
import com.outr.solr4s.{Field, FieldType, IndexedCollection, SolrIndexed}
import com.outr.solr4s.query._
import io.circe.Json
import io.circe.generic.auto._
import io.youi.Unique
import org.scalatest.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import profig.JsonUtil

class SimpleSpec extends AsyncWordSpec with Matchers {
  private val collectionName: String = s"solr4s-people"

  "Simple spec" should {
    val newYorkCity = "New York City"
    val chicago = "Chicago"
    val jeffersonValley = "Jefferson Valley"
    val noble = "Noble"
    val oklahomaCity = "Oklahoma City"
    val yonkers = "Yonkers"
    val specialCity = "OR"

    val adam = Person("1", "Adam", "adam@solr4s", Some(21), 1.23, 4321L, List(newYorkCity, yonkers), enabled = true)
    val bethany = Person("2", "Bethany", "bethany@solr4s", Some(22), 1.24, 54321L, Nil, enabled = false)
    val charlie = Person("3", "Charlie", "charlie@solr4s", Some(20), 1.25, 34321L, List(chicago, jeffersonValley), enabled = true)
    val debbie = Person("4", "Debbie", "debbie@solr4s", None, 1.26, 64321L, List(noble, oklahomaCity, newYorkCity, specialCity), enabled = false)

    "verify the collections" in {
      Indexed.collections.map(_.collectionName) should be(List(collectionName))
    }
    "create the collection" in {
      Indexed.person.collectionName should be(collectionName)
      Indexed.createNonExistent().map { _ =>
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
        results.docs.map(_.entry) should be(List(adam))
        results.maxScore should be(1.0)
        results.docs.head.score should be(1.0)
        results.docs.head.id should be("1")
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
        results.docs.map(_.entry).toSet should be(Set(adam, bethany, charlie, debbie))
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
          results.docs.map(_.entry) should be(List(bethany))
          results.docs.head.id should be("2")
          results.docs.head.version should be > 0L
        }
    }
    "query back by age" in {
      Indexed
        .person
        .query(Indexed.person.age === 21)
        .execute()
        .map { results =>
          results.docs.map(_.entry.name) should be(List("Adam"))
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
          results.docs.map(_.entry.name).toSet should be(Set("Debbie", "Charlie", "Adam", "Bethany"))
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
          results.docs.map(_.entry.name) should be(List("Adam", "Debbie"))
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
          results.docs.map(_.entry.name) should be(List("Charlie"))
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
          results.docs.map(_.entry) should be(List(
            SimplePerson("Adam"),
            SimplePerson("Bethany"),
            SimplePerson("Charlie"),
            SimplePerson("Debbie")
          ))
        }
    }
    "verify the collection exists" in {
      Indexed.client.api.collections.list().map { list =>
        list.collections should contain(collectionName)
      }
    }
    "update name by id" in {
      Indexed
        .person
        .update(bethany.copy(name = "Not Bethany"))
        .commit()
        .execute()
        .map { response =>
          response.successOrException() should be(true)
        }
    }
    "query back all records with renamed value" in {
      Indexed.person.query.execute().map { results =>
        results.total should be(4)
        results.docs.map(_.entry.name).toSet should be(Set(adam.name, charlie.name, debbie.name, "Not Bethany"))
        results.maxScore should be(1.0)
      }
    }
    "delete by query" in {
      Indexed.person
        .delete(Indexed.person.name.filter("Not Bethany"))
        .commit()
        .execute()
        .map { response =>
          response.successOrException() should be(true)
        }
    }
    "query back all except the deleted person" in {
      Indexed.person.query.execute().map { results =>
        results.total should be(3)
        results.docs.map(_.entry).toSet should be(Set(adam, charlie, debbie))
        results.maxScore should be(1.0)
      }
    }
    "update Adam's email address and add Oklahoma City" in {
      Indexed
        .person
        .modify("1")
        .set(Indexed.person.email, "adam@modified")
        .add(Indexed.person.cities, List(oklahomaCity))
        .batch()
        .commit()
        .execute()
        .map { response =>
          response.successOrException() should be(true)
        }
    }
    "query back Adam and verify new email address" in {
      Indexed
        .person
        .query(Indexed.person.name === "adam")
        .execute()
        .map { results =>
          results.total should be(1)
          results.docs.map(_.entry) should be(List(adam.copy(
            email = "adam@modified",
            cities = List(newYorkCity, yonkers, oklahomaCity)
          )))
          results.docs.head.id should be("1")
          results.docs.head.version should be > 0L
        }
    }
    "query back special city using a keyword as a value" in {
      Indexed
        .person
        .query(Indexed.person.cities === "OR")
        .execute()
        .map { results =>
          results.total should be(1)
          results.docs.head.id should be("4")
        }
    }
    "query back with stats" in {
      Indexed.person.query.stats(Indexed.person.age).statsCalculateDistinct().execute().map { results =>
        results.stats(Indexed.person.age).distinctValues should be(List(20, 21))
        results.total should be(3)
      }
    }
    "delete the collection" in {
      Indexed.delete().map { _ =>
        succeed
      }
    }
    "verify the collection no longer exists" in {
      Indexed.client.api.collections.list().map { list =>
        list.collections should not contain collectionName
      }
    }
  }

  // TODO: SBT plugin like giant-scala?

  case class Person(id: String,
                    name: String,
                    email: String,
                    age: Option[Int],
                    progress: Double,
                    bytes: Long,
                    cities: List[String] = Nil,
                    enabled: Boolean)

  case class SimplePerson(name: String)

  class PersonIndex(override val solr: SolrIndexed) extends IndexedCollection[Person] {
    val id: Field[String] = Field[String]("id", FieldType.String)
    val name: Field[String] = Field[String]("name", FieldType.TextEnglish)
    val email: Field[String] = Field[String]("email", FieldType.TextEnglish)
    val age: Field[Int] = Field[Int]("age", FieldType.IntPoint)
    val progress: Field[Double] = Field[Double]("progress", FieldType.DoublePoint)
    val bytes: Field[Long] = Field[Long]("bytes", FieldType.LongPoint)
    val cities: Field[List[String]] = Field[List[String]]("cities", FieldType.String, multiValued = true)
    val enabled: Field[Boolean] = Field[Boolean]("enabled", FieldType.Boolean)

    override def collectionName: String = SimpleSpec.this.collectionName

    override def toJSON(i: Person): Json = JsonUtil.toJson[Person](i)
    override def fromJSON(json: Json): Person = JsonUtil.fromJson[Person](json)

    override def fields: List[Field[_]] = List(name, age, progress, bytes, enabled, email)
  }

  object Indexed extends SolrIndexed() {
    val person: PersonIndex = new PersonIndex(this)
  }
}