package com.outr.solr4s

import com.outr.solr4s.admin.SolrCollection
import io.circe.Json

import scala.concurrent.Future
import scribe.Execution.global

trait IndexedCollection[I] {
  protected def solr: SolrIndexed
  protected lazy val solrCollection: SolrCollection = solr.client.api.collection(collectionName)

  SolrIndexed.add(solr, this)

  def fields: List[Field[_]]

  def collectionName: String = getClass.getSimpleName.toLowerCase.replaceAllLiterally("index", "")
  def toJSON(i: I): Json
  def fromJSON(json: Json): I

  private lazy val bo: BatchOperations[I] = new BatchOperations[I](this, solrCollection)

  def add(docs: I*): BatchOperations[I] = bo.add(docs: _*)
  def delete(id: Option[String] = None, query: Option[String] = None): BatchOperations[I] = bo.delete(id, query)
  def commit(): BatchOperations[I] = bo.commit()
  def optimize(waitSearcher: Boolean = false): BatchOperations[I] = bo.optimize(waitSearcher)

  lazy val query: QueryBuilder[I] = QueryBuilder[I](fromJSON, solrCollection.query)

  def create(numShards: Int, deleteSchema: Boolean = true, createSchema: Boolean = true): Future[Unit] = for {
    // Create the collection
    _ <- solrCollection.admin.create(numShards = numShards, waitForFinalState = true).map { response =>
      if (!response.isSuccess) {
        throw new RuntimeException(s"Failed to create collection $collectionName. ${response.error}")
      }
    }
    // Load collection info
    info <- solrCollection.admin.schema.info
    // Delete existing copy fields
    _ <- if (deleteSchema) {
      val schema = info.schema.copyFields.foldLeft(solrCollection.admin.schema) {
        case (s, f) => s.deleteCopyField(f.source, f.dest)
      }
      if (schema.nonEmpty) {
        schema.execute().map { response =>
          if (!response.isSuccess) {
            throw new RuntimeException(s"Failed to delete copy fields for $collectionName. ${response.error}")
          }
        }
      } else {
        Future.successful(())
      }
    } else {
      Future.successful(())
    }
    // Delete existing fields
    _ <- if (deleteSchema) {
      val schema = info.schema.fields.filterNot(f => f.name.startsWith("_") || f.name == "id").foldLeft(solrCollection.admin.schema) {
        case (s, f) => s.deleteField(f.name)
      }
      schema.execute().map { response =>
        if (!response.isSuccess) {
          throw new RuntimeException(s"Failed to delete fields for $collectionName. ${response.error}")
        }
      }
    } else {
      Future.successful(())
    }
    // Create new fields
    _ <- if (createSchema) {
      val schema = fields.foldLeft(solrCollection.admin.schema) {
        case (s, f) => {
          s.addField(
            name = f.name,
            `type` = f.`type`,
            default = f.default,
            indexed = f.indexed,
            stored = f.stored,
            docValues = f.docValues,
            sortMissingFirst = f.sortMissingFirst,
            sortMissingLast = f.sortMissingLast,
            multiValued = f.multiValued,
            uninvertible = f.uninvertible,
            omitNorms = f.omitNorms,
            omitTermFreqAndPositions = f.omitTermFreqAndPositions,
            omitPositions = f.omitPositions,
            termVectors = f.termVectors,
            termPositions = f.termPositions,
            termOffsets = f.termOffsets,
            termPayloads = f.termPayloads,
            required = f.required,
            useDocValuesAsStored = f.useDocValuesAsStored,
            large = f.large
          )
        }
      }
      schema.execute().map { response =>
        if (!response.isSuccess) {
          throw new RuntimeException(s"Failed to create fields for $collectionName. ${response.error}")
        }
      }
    } else {
      Future.successful(())
    }
  } yield {
    ()
  }

  def delete(): Future[Unit] = {
    solrCollection.admin.delete().map { response =>
      if (!response.isSuccess) {
        throw new RuntimeException(s"Failed to delete collection $collectionName. ${response.error}")
      }
    }
  }
}