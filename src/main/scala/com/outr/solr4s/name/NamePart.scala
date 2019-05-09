package com.outr.solr4s.name

case class NamePart(`type`: NamePartType, value: String) {
  def is(`type`: NamePartType): Boolean = this.`type` == `type`

  lazy val simple: String = value.toLowerCase.replaceAll("[.,']", "")

  override def toString: String = s"${`type`}: $value"
}