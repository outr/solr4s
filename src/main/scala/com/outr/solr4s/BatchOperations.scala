package com.outr.solr4s

import com.outr.solr4s.admin.{GeneralResponse, UpdateInterface}
import com.outr.solr4s.query.Query

import scala.concurrent.{ExecutionContext, Future}

class BatchOperations[I](collection: IndexedCollection[I], val update: UpdateInterface) {
  private[solr4s] def u(update: UpdateInterface): BatchOperations[I] = new BatchOperations[I](collection, update)

  def add(docs: I*): BatchOperations[I] = {
    u(docs.foldLeft(update)((ui, i) => ui.add(collection.toJSON(i))))
  }
  def update(docs: I*): BatchOperations[I] = {
    u(docs.foldLeft(update)((ui, i) => ui.add(collection.toJSON(i), overwrite = Some(true))))
  }
  def commit(): BatchOperations[I] = u(update.commit())
  def optimize(waitSearcher: Boolean = false): BatchOperations[I] = u(update.optimize(waitSearcher))
  def delete(id: Option[String] = None, query: Option[Query] = None): BatchOperations[I] = u(update.delete(id, query))
  def modify(id: String): ModifyBuilder[I] = ModifyBuilder[I](this, id, Nil)

  def execute()(implicit ec: ExecutionContext): Future[GeneralResponse] = update.execute()(ec)
}
