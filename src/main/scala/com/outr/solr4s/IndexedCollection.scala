package com.outr.solr4s

import com.outr.solr4s.admin.{SolrCollection, SolrUpdateInstruction}
import com.outr.solr4s.query.Query
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
  def update(docs: I*): BatchOperations[I] = bo.update(docs: _*)
  def delete(id: String): BatchOperations[I] = bo.delete(Some(id), None)
  def delete(query: Query): BatchOperations[I] = bo.delete(None, Some(query))
  def commit(): BatchOperations[I] = bo.commit()
  def optimize(waitSearcher: Boolean = false): BatchOperations[I] = bo.optimize(waitSearcher)
  def modify(id: String): ModifyBuilder[I] = ModifyBuilder[I](bo, id, Nil)

  lazy val query: QueryBuilder[I] = QueryBuilder[I](fromJSON, solrCollection.query)

  def create(routerName: String = "",
             numShards: Int = 1,
             shards: List[String] = Nil,
             replicationFactor: Int = 1,
             maxShardsPerNode: Int = 1,
             createNodeSet: List[String] = Nil,
             createNodeSetShuffle: Boolean = true,
             collectionConfigName: String = "_default",
             routerField: String = "",
             propertyName: String = "",
             autoAddReplicas: Boolean = false,
             async: String = "",
             rule: String = "",
             snitch: String = "",
             waitForFinalState: Boolean = true,
             deleteSchema: Boolean = true,
             createSchema: Boolean = true): Future[Unit] = for {
    // Create the collection
    _ <- solrCollection.admin.create(
      routerName = routerName,
      numShards = numShards,
      shards = shards,
      replicationFactor = replicationFactor,
      maxShardsPerNode = maxShardsPerNode,
      createNodeSet = createNodeSet,
      createNodeSetShuffle = createNodeSetShuffle,
      collectionConfigName = collectionConfigName,
      routerField = routerField,
      propertyName = propertyName,
      autoAddReplicas = autoAddReplicas,
      async = async,
      rule = rule,
      snitch = snitch,
      waitForFinalState = waitForFinalState
    ).map { response =>
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
      if (schema.nonEmpty) {
        schema.execute().map { response =>
          if (!response.isSuccess) {
            throw new RuntimeException(s"Failed to delete fields for $collectionName. ${response.error}")
          }
        }
      } else {
        Future.successful(())
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