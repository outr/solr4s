package spec

import com.outr.solr4s.FieldType
import com.outr.solr4s.query.{Query, QueryValue, TermQuery}
import com.outr.solr4s.admin.{Direction, SolrClient, Sort}
import org.scalatest.{AsyncWordSpec, Matchers}
import profig.JsonUtil

import scala.concurrent.Future

class AdministrationSpec extends AsyncWordSpec with Matchers {
  "Administration" should {
    lazy val client = SolrClient()
    lazy val collection1 = client.api.collection("solr4s_admin")

    var create: Boolean = false

    "create a collection" in {
      client.api.collections.list().flatMap { list =>
        if (list.collections.contains(collection1.collectionName)) {
          Future.successful(succeed)
        } else {
          create = true
          collection1.admin.create(waitForFinalState = true).map { r =>
            r.isSuccess should be(true)
            r.success.keys.size should be(1)
          }
        }
      }
    }
    "list the collections and verify the new collection" in {
      client.api.collections.list().map { list =>
        list.collections should contain(collection1.collectionName)
      }
    }
    "add fields to the collection" in {
      if (create) {
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
      } else {
        succeed
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
    "simple facet query" in {
      collection1
        .query
        .facet("name")
        .facet("id")
        .execute()
        .map { r =>
          val buckets = r.facet("name")
          buckets.length should be(4)
          buckets.map(_.`val`).toSet should be(Set("adam", "bethani", "charli", "debbi"))
        }
    }
    "delete all records" in {
      collection1.delete(query = Some(Query.all)).commit().execute().map { r =>
        r.isSuccess should be(true)
      }
    }
    "query back zero records" in {
      collection1
        .query
        .execute()
        .map { r =>
          r.response.numFound should be(0)
          r.response.docs.length should be(0)
        }
    }
  }

  case class Name(id: String, name: String)
}