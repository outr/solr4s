package com.outr.solr4s

import com.outr.solr4s.admin.{GeneralResponse, UpdateInterface}

import scala.concurrent.{ExecutionContext, Future}

class BatchOperations[I](collection: IndexedCollection[I], update: UpdateInterface) {
  private def u(update: UpdateInterface): BatchOperations[I] = new BatchOperations[I](collection, update)

  def add(docs: I*): BatchOperations[I] = {
    u(docs.foldLeft(update)((ui, i) => ui.add(collection.toJSON(i))))
  }
  def commit(): BatchOperations[I] = u(update.commit())
  def optimize(waitSearcher: Boolean = false): BatchOperations[I] = u(update.optimize(waitSearcher))
  def delete(id: Option[String] = None, query: Option[String] = None): BatchOperations[I] = u(update.delete(id, query))

  def execute()(implicit ec: ExecutionContext): Future[GeneralResponse] = update.execute()(ec)
}
