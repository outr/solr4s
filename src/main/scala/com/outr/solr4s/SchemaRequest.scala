package com.outr.solr4s

case class SchemaRequest(instructions: List[SchemaInstruction] = Nil) {
  def isEmpty: Boolean = instructions.isEmpty
  def nonEmpty: Boolean = instructions.nonEmpty
}
