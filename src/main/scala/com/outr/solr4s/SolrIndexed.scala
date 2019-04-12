package com.outr.solr4s

import com.outr.solr4s.admin.SolrClient
import io.youi.net._

import scala.concurrent.Future
import scala.language.experimental.macros
import scribe.Execution.global

class SolrIndexed(url: URL = url"http://localhost:8983") {
  lazy val client = SolrClient(url)

  private var _collections = List.empty[IndexedCollection[Any]]
  def collections: List[IndexedCollection[Any]] = _collections

  protected def add[I](collection: IndexedCollection[I]): Unit = synchronized {
    _collections = _collections ::: List(collection.asInstanceOf[IndexedCollection[Any]])
  }

  def create(numShards: Int = 1): Future[Unit] = {
    Future.sequence(collections.map(_.create(numShards))).map(_ => ())
  }

  def delete(): Future[Unit] = {
    Future.sequence(collections.map(_.delete())).map(_ => ())
  }
}

object SolrIndexed {
  def add[I](indexed: SolrIndexed, collection: IndexedCollection[I]): Unit = indexed.add(collection)
}