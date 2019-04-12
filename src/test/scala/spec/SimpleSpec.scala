package spec

import com.outr.solr4s.admin.FieldType
import com.outr.solr4s.{Field, IndexedCollection, SolrIndexed}
import io.circe.Json
import org.scalatest.{AsyncWordSpec, Matchers}
import profig.JsonUtil

class SimpleSpec extends AsyncWordSpec with Matchers {
  "Simple spec" should {
    val adam = Person("Adam", "adam@solr4s", 21, 1.23, 4321L, enabled = true)

    "verify the collections" in {
      Indexed.collections.map(_.collectionName) should be(List("person"))
    }
    "create the collection" in {
      Indexed.person.collectionName should be("person")
      Indexed.create(2).map { _ =>
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
    // TODO: Insert a batch of documents
    // TODO: Query back a document
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
                    enabled: Boolean)

  class PersonIndex(override val solr: SolrIndexed) extends IndexedCollection[Person] {
    val name: Field[String] = Field[String]("name", FieldType.TextEnglish)
    val age: Field[Int] = Field[Int]("age", FieldType.IntPoint)
    val progress: Field[Double] = Field[Double]("progress", FieldType.DoublePoint)
    val bytes: Field[Long] = Field[Long]("bytes", FieldType.LongPoint)
    val enabled: Field[Boolean] = Field[Boolean]("enabled", FieldType.Boolean)
    val email: Field[String] = Field[String]("email", FieldType.TextEnglish)

    override def toJSON(i: Person): Json = JsonUtil.toJson[Person](i)
    override def fromJSON(json: Json): Person = JsonUtil.fromJson[Person](json)

    override def fields: List[Field[_]] = List(name, age, progress, bytes, enabled, email)
  }

  object Indexed extends SolrIndexed() {
    val person: PersonIndex = new PersonIndex(this)
  }
}