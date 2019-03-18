package spec

import com.outr.solr4s.{FieldType, SolrClient}
import org.scalatest.{AsyncWordSpec, Matchers}
import profig.JsonUtil

class AdministrationSpec extends AsyncWordSpec with Matchers {
  "Administration" should {
    lazy val client = SolrClient()
    lazy val collection1 = client.api.collection("administrationSpec")

    "create a collection" in {
      collection1.admin.create(numShards = 2, waitForFinalState = true).map { r =>
        r.isSuccess should be(true)
        r.success.keys.size should be(2)
      }
    }
    "list the collections and verify the new collection" in {
      client.api.collections.list().map { list =>
        list.collections should contain(collection1.collectionName)
      }
    }
    "add fields to the collection" in {
      collection1.admin.schema
        .deleteField("name")
        .addField("name", FieldType.TextEnglish)
        .execute().map { r =>
        if (!r.isSuccess) fail(JsonUtil.toJsonString(r))
        r.isSuccess should be(true)
      }
    }
    "insert a few documents" in {
      collection1
        .add(JsonUtil.toJson(Name("1", "Adam")))
        .add(JsonUtil.toJson(Name("2", "Bethany")))
        .add(JsonUtil.toJson(Name("3", "Charlie")))
        .add(JsonUtil.toJson(Name("4", "Debbie")))
        .commit()
        .execute()
        .map { r =>
          r.isSuccess should be(true)
        }
    }
    "delete the collection" in {
      collection1.admin.delete().map { r =>
        r.isSuccess should be(true)
      }
    }
    "list the collections and verify the collection was removed" in {
      client.api.collections.list().map { list =>
        list.collections should not contain "administrationSpec"
      }
    }
  }

  case class Name(id: String, name: String)
}
