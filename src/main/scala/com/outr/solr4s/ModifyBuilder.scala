package com.outr.solr4s

import com.outr.solr4s.admin.SolrUpdateInstruction
import io.circe.Json

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

  def set[T](field: Field[T], values: T*): ModifyBuilder[I] = {
    withOperation(field.name, "set", values2Json(values))
  }

  def add[T](field: Field[T], values: T*): ModifyBuilder[I] = {
    withOperation(field.name, "add", values2Json(values))
  }

  def remove[T](field: Field[T], values: T*): ModifyBuilder[I] = {
    withOperation(field.name, "remove", values2Json(values))
  }

  def removeRegex[T](field: Field[T], values: String*): ModifyBuilder[I] = {
    withOperation(field.name, "removeregex", values2Json(values))
  }

  def increment[T](field: Field[T], amount: T): ModifyBuilder[I] = {
    withOperation(field.name, "inc", value2Json(amount))
  }

  def batch(): BatchOperations[I] = bo.u(bo.update.modify(this))

  private def values2Json(values: Seq[Any]): Json = {
    val jsonValues = values.map(value2Json)
    if (jsonValues.isEmpty) {
      Json.obj()
    } else if (jsonValues.tail.nonEmpty) {
      Json.arr(jsonValues: _*)
    } else {
      jsonValues.head
    }
  }

  private def value2Json(value: Any): Json = value match {
    case s: String => Json.fromString(s)
    case seq: Seq[_] => Json.arr(seq.map(value2Json): _*)
    case _ => throw new RuntimeException(s"Unsupported type: $value (${value.getClass.getSimpleName})")
  }
}
