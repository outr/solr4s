package com.outr.solr4s.admin

case class CollectionSchema(name: String,
                            version: Double,
                            uniqueKey: String,
                            fieldTypes: List[CollectionFieldType],
                            fields: List[CollectionField],
                            dynamicFields: List[CollectionDynamicField],
                            copyFields: List[CollectionCopyField])
