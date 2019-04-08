package com.outr.solr4s

import io.circe.Json
import io.youi.client.HttpClient
import io.youi.http.content.Content
import io.youi.net.ContentType

import scala.concurrent.{ExecutionContext, Future}

trait UpdateInterface {
  protected def updateClient: HttpClient
  def instructions: List[SolrUpdateInstruction]
  def withInstruction(instruction: SolrUpdateInstruction): UpdateInterface
  def execute()(implicit ec: ExecutionContext): Future[GeneralResponse] = {
    val jsonString = SolrAPI.jsonObj(instructions.map(i => i.key -> i.value))
    println(s"JSON STRING: $jsonString")
    updateClient
      .content(Content.string(jsonString, ContentType.`application/json`))
      .post
      .call[GeneralResponse]
  }

  def add(doc: Json,
          commitWithin: Option[Long] = None,
          overwrite: Option[Boolean] = None): UpdateInterface = {
    withInstruction(DocumentAdd(doc, commitWithin, overwrite))
  }
  def commit(): UpdateInterface = withInstruction(CommitInstruction)
  def optimize(waitSearcher: Boolean = false): UpdateInterface = withInstruction(OptimizeInstruction(waitSearcher))
  def delete(id: Option[String] = None, query: Option[String] = None): UpdateInterface = {
    withInstruction(DeleteInstruction(id, query))
  }
}
