package com.outr.solr4s

case class CollectionSchema(name: String,
                            version: Double,
                            uniqueKey: String,
                            fieldTypes: List[CollectionFieldType],
                            fields: List[CollectionField],
                            dynamicFields: List[CollectionDynamicField],
                            copyFields: List[CollectionCopyField])
