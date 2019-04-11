package com.outr.solr4s

import com.outr.solr4s.admin.SolrCollection

import scala.concurrent.Future
import scribe.Execution.global

trait IndexedCollection[I] {
  protected def solr: SolrIndexed
  protected lazy val solrCollection: SolrCollection = solr.client.api.collection(collectionName)

  SolrIndexed.add(solr, this)

  def fields: List[Field[_]]

  def collectionName: String = getClass.getSimpleName.toLowerCase.replaceAllLiterally("index", "")

  def create(numShards: Int): Future[Unit] = {
    solrCollection.admin.create(numShards = numShards, waitForFinalState = true).map { response =>
      if (!response.isSuccess) {
        throw new RuntimeException(s"Failed to create collection $collectionName. ${response.error}")
      }
    }
  }
  def delete(): Future[Unit] = {
    solrCollection.admin.delete().map { response =>
      if (!response.isSuccess) {
        throw new RuntimeException(s"Failed to delete collection $collectionName. ${response.error}")
      }
    }
  }
}