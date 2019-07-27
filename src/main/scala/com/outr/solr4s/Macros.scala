package com.outr.solr4s

import scala.annotation.compileTimeOnly
import scala.reflect.macros.blackbox

@compileTimeOnly("Enable macro paradise to expand compile-time macros")
object Macros {
  def modifySet[T](c: blackbox.Context)(field: c.Tree, values: c.Tree*)(implicit t: c.WeakTypeTag[T]): c.Tree = {
    withOperation[T](c)("set", field, values: _*)(t)
  }

  def modifyAdd[T](c: blackbox.Context)(field: c.Tree, values: c.Tree*)(implicit t: c.WeakTypeTag[T]): c.Tree = {
    withOperation[T](c)("add", field, values: _*)(t)
  }

  def modifyAddDistinct[T](c: blackbox.Context)(field: c.Tree, values: c.Tree*)(implicit t: c.WeakTypeTag[T]): c.Tree = {
    withOperation[T](c)("add-distinct", field, values: _*)(t)
  }

  def modifyRemove[T](c: blackbox.Context)(field: c.Tree, values: c.Tree*)(implicit t: c.WeakTypeTag[T]): c.Tree = {
    withOperation[T](c)("remove", field, values: _*)(t)
  }

  def modifyIncrement[T](c: blackbox.Context)(field: c.Tree, amount: c.Tree)(implicit t: c.WeakTypeTag[T]): c.Tree = {
    withOperation[T](c)("increment", field, amount)(t)
  }

  private def withOperation[T](c: blackbox.Context)
                              (op: String, field: c.Tree, values: c.Tree*)
                              (implicit t: c.WeakTypeTag[T]): c.Tree = {
    import c.universe._

    val json = if (values.isEmpty) {
      q"Json.Null"
    } else if (values.tail.isEmpty) {
      q"JsonUtil.toJson[$t](${values.head})"
    } else {
      q"Json.arr(..${values.map(v => q"JsonUtil.toJson[$t](v)")})"
    }
    val builder = c.prefix.tree
    q"""
       import _root_.profig._
       import _root_.io.circe.Json
       $builder.withOperation($field.name, $op, $json)
      """
  }
}
