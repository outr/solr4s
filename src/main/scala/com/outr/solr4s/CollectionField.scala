package com.outr.solr4s

case class CollectionField(name: String, `type`: String, indexed: Boolean, stored: Boolean, multiValued: Boolean = false)
