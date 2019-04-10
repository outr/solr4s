package com.outr.solr4s.admin

import io.youi.net._

import scala.concurrent.{ExecutionContext, Future}

class SolrCollectionAdmin(val collectionName: String, api: SolrAPI) {
  lazy val schema: SolrSchema = SolrSchema(this, api)

  def create(routerName: String = "",
             numShards: Int = -1,
             shards: List[String] = Nil,
             replicationFactor: Int = 1,
             maxShardsPerNode: Int = 1,
             createNodeSet: List[String] = Nil,
             createNodeSetShuffle: Boolean = true,
             collectionConfigName: String = "",
             routerField: String = "",
             propertyName: String = "",
             autoAddReplicas: Boolean = false,
             async: String = "",
             rule: String = "",
             snitch: String = "",
             waitForFinalState: Boolean = false)
            (implicit ec: ExecutionContext): Future[GeneralResponse] = {
    api
      .client
      .path(path"/solr/admin/collections")
      .params("action" -> "CREATE", "name" -> collectionName)
      .param("routerName", routerName, "")
      .param("numShards", numShards, -1)
      .param("shards", shards, Nil)
      .param("replicationFactor", replicationFactor, 1)
      .param("maxShardsPerNode", maxShardsPerNode, 1)
      .param("createNodeSet", createNodeSet, Nil)
      .param("createNodeSetShuffle", createNodeSetShuffle, true)
      .param("collectionConfigName", collectionConfigName, "")
      .param("routerField", routerField, "")
      .param("propertyName", propertyName, "")
      .param("autoAddReplicas", autoAddReplicas, false)
      .param("async", async, "")
      .param("rule", rule, "")
      .param("snitch", snitch, "")
      .param("waitForFinalState", waitForFinalState, false)
      .call[GeneralResponse]
  }

  def modify(maxShardsPerNode: Option[Int] = None,
             replicationFactor: Option[Int] = None,
             autoAddReplicas: Option[Boolean] = None,
             collectionConfigName: Option[String] = None,
             rule: Option[String] = None,
             snitch: Option[String] = None)
            (implicit ec: ExecutionContext): Future[GeneralResponse] = api
    .client
    .path(path"/solr/admin/collections")
    .params("action" -> "MODIFYCOLLECTION", "collection" -> collectionName)
    .param("maxShardsPerNode", maxShardsPerNode, None)
    .param("replicationFactor", replicationFactor, None)
    .param("autoAddReplicas", autoAddReplicas, None)
    .param("collectionConfigName", collectionConfigName, None)
    .param("rule", rule, None)
    .param("snitch", snitch, None)
    .call[GeneralResponse]

  def backup(name: String,
             location: String,
             async: String = "",
             repository: String = "")
            (implicit ec: ExecutionContext): Future[GeneralResponse] = api
    .client
    .path(path"/solr/admin/collections")
    .params("action" -> "BACKUP", "name" -> name, "collection" -> collectionName, "location" -> location)
    .param("async", async, "")
    .param("repository", repository, "")
    .call[GeneralResponse]

  def restore(name: String,
              location: String,
              async: String = "",
              repository: String = "")
             (implicit ec: ExecutionContext): Future[GeneralResponse] = api
    .client
    .path(path"/solr/admin/collections")
    .params("action" -> "RESTORE", "name" -> name, "collection" -> collectionName, "location" -> location)
    .param("async", async, "")
    .param("repository", repository, "")
    .call[GeneralResponse]

  def status(requestId: String)(implicit ec: ExecutionContext): Future[GeneralResponse] = api
    .client
    .path(path"/solr/admin/collections")
    .params("action" -> "REQUESTSTATUS", "requestid" -> requestId, "wt" -> "json")
    .call[GeneralResponse]

  def deleteStatus(requestId: String, flush: Boolean = false)
                  (implicit ec: ExecutionContext): Future[GeneralResponse] = api
    .client
    .path(path"/solr/admin/collections")
    .params("action" -> "DELETESTATUS", "requestid" -> requestId, "wt" -> "json")
    .param("flush", flush, false)
    .call[GeneralResponse]

  def delete(shard: String = "", replica: String = "")
            (implicit ec: ExecutionContext): Future[GeneralResponse] = {
    api
      .client
      .path(path"/solr/admin/collections")
      .params("action" -> "DELETE", "name" -> collectionName)
      .param("shard", shard, "")
      .param("replica", replica, "")
      .call[GeneralResponse]
  }
}
