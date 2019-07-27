package com.outr.solr4s

import com.outr.solr4s.admin.SolrUpdateInstruction
import io.circe.Json
import profig.JsonUtil

import scala.language.experimental.macros

case class ModifyBuilder[I](bo: BatchOperations[I], id: String, ops: List[ModifyOperation]) extends SolrUpdateInstruction{
  override def key: String = "add"

  override def value: Json = {
    val doc = ops.foldLeft(Json.obj("id" -> Json.fromString(id)))((json, op) => {
      json.deepMerge(op.json)
    })
    Json.obj("overwrite" -> Json.True, "doc" -> doc)
  }

  def withOperation(field: String, operation: String, value: Json): ModifyBuilder[I] = {
    copy(ops = ops ::: List(ModifyOperation(field, operation, value)))
  }

  def set[T](field: Field[T], values: T*): ModifyBuilder[I] = macro Macros.modifySet[T]

  def add[T](field: Field[T], values: T*): ModifyBuilder[I] = macro Macros.modifyAdd[T]

  def addDistinct[T](field: Field[T], values: T*): ModifyBuilder[I] = macro Macros.modifyAddDistinct[T]

  def remove[T](field: Field[T], values: T*): ModifyBuilder[I] = macro Macros.modifyRemove[T]

  def removeRegex[T](field: Field[T], values: String*): ModifyBuilder[I] = {
    withOperation(field.name, "removeregex", JsonUtil.toJson(values))
  }

  def increment[T](field: Field[T], amount: T): ModifyBuilder[I] = macro Macros.modifyIncrement[T]

  def batch(): BatchOperations[I] = bo.u(bo.update.modify(this))
}