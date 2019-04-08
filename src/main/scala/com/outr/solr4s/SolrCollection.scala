package com.outr.solr4s

import io.youi.client.HttpClient
import io.youi.net.Path

class SolrCollection(val collectionName: String, val api: SolrAPI) extends UpdateInterface {
  lazy val admin: SolrCollectionAdmin = new SolrCollectionAdmin(collectionName, api)

  override protected val updateClient: HttpClient = api.client.path(Path.parse(s"/solr/$collectionName/update"))

  lazy val query: SolrQuery = SolrQuery(collection = this)

  override def instructions: List[SolrUpdateInstruction] = Nil

  override def withInstruction(instruction: SolrUpdateInstruction): UpdateInterface = {
    SolrUpdateCommand(updateClient, List(instruction))
  }
}
