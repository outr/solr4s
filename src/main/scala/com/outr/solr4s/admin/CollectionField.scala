package com.outr.solr4s.admin

case class CollectionField(name: String, `type`: String, indexed: Boolean = false, stored: Boolean = false, multiValued: Boolean = false)
