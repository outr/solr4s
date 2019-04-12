package spec

import com.outr.solr4s.query.{QueryValue, TermQuery}
import com.outr.solr4s.admin.{Direction, FieldType, SolrClient, Sort}
import org.scalatest.{AsyncWordSpec, Matchers}
import profig.JsonUtil

class AdministrationSpec extends AsyncWordSpec with Matchers {
  "Administration" should {
    lazy val client = SolrClient()
    lazy val collection1 = client.api.collection("solr4s_admin")

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
      collection1.admin.schema.info.flatMap { response =>
        val schema = if (response.schema.fields.exists(_.name == "name")) {
          collection1.admin.schema.deleteField("name")
        } else {
          collection1.admin.schema
        }

        schema
          .addField("name", FieldType.TextEnglish)
          .execute().map { r =>
          if (!r.isSuccess) fail(JsonUtil.toJsonString(r))
          r.isSuccess should be(true)
        }
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
    "query back all records" in {
      collection1
        .query
        .execute()
        .map { r =>
        r.response.numFound should be(4)
        r.response.docs.length should be(4)
      }
    }
    "query back all records sorted by name" in {
      collection1
        .query
        .sort(Sort("name", Direction.Descending))
        .execute()
        .map { r =>
          r.response.numFound should be(4)
          val names = r.response.docs.flatMap(j => (j \\ "name").head.asString)
          names should be(List("Debbie", "Charlie", "Bethany", "Adam"))
        }
    }
    "query back one record" in {
      collection1
        .query(TermQuery(QueryValue("debbie"), field = Some("name")))
        .execute()
        .map { r =>
          r.response.numFound should be(1)
          (r.response.docs.head \\ "name").head.asString should be(Some("Debbie"))
          r.response.docs.length should be(1)
      }
    }
    "filter back one record" in {
      collection1
        .query
        .filter(TermQuery(QueryValue("debbie"), field = Some("name")))
        .execute()
        .map { r =>
          r.response.numFound should be(1)
          (r.response.docs.head \\ "name").head.asString should be(Some("Debbie"))
          r.response.docs.length should be(1)
        }
    }
    "filter back one record with a param" in {
      collection1
        .query
        .filter(TermQuery(QueryValue("${name}"), field = Some("name")))
        .params("name" -> "bethany")
        .execute()
        .map { r =>
          r.response.numFound should be(1)
          (r.response.docs.head \\ "name").head.asString should be(Some("Bethany"))
          r.response.docs.length should be(1)
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