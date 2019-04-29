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

  def create(collections: List[IndexedCollection[_]] = collections,
             routerName: String = "",
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
             waitForFinalState: Boolean = false): Future[Unit] = {
    Future.sequence(collections.map(_.create(
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
    ))).map(_ => ())
  }

  def createNonExistent(routerName: String = "",
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
                        waitForFinalState: Boolean = false): Future[List[String]] = for {
    collectionNames <- client.api.collections.list().map(_.collections.toSet)
    newCollections = collections.filterNot(c => collectionNames.contains(c.collectionName))
    _ <- create(
      collections = newCollections,
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
    )
  } yield {
    newCollections.map(_.collectionName).sorted
  }

  def delete(): Future[Unit] = {
    Future.sequence(collections.map(_.delete())).map(_ => ())
  }
}

object SolrIndexed {
  def add[I](indexed: SolrIndexed, collection: IndexedCollection[I]): Unit = indexed.add(collection)
}