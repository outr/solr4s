package com.outr.solr4s.admin

import io.youi.http.content.Content
import io.youi.net.{ContentType, Path}

import scala.concurrent.{ExecutionContext, Future}

case class SolrSchema(collection: SolrCollectionAdmin, api: SolrAPI, request: SchemaRequest = SchemaRequest()) {
  private val client = api.client.path(Path.parse(s"/solr/${collection.collectionName}/schema"))

  def isEmpty: Boolean = request.isEmpty
  def nonEmpty: Boolean = request.nonEmpty

  private def o[T](value: T, default: T): Option[T] = if (value != default) {
    Some(value)
  } else {
    None
  }

  def info(implicit ec: ExecutionContext): Future[CollectionSchemaResponse] = client.call[CollectionSchemaResponse]

  def execute()(implicit ec: ExecutionContext): Future[GeneralResponse] = {
    assert(nonEmpty, "No schema changes supplied")
    val jsonString = SolrAPI.jsonObj(request.instructions.map(i => i.json))
    client
      .post
      .content(Content.string(jsonString, ContentType.`application/json`))
      .call[GeneralResponse]
  }

  def withInstruction(instruction: SchemaInstruction): SolrSchema = copy(request = request.copy(request.instructions ::: List(instruction)))

  def addField(name: String,
               `type`: FieldType,
               default: String = "",
               indexed: Boolean = true,
               stored: Boolean = true,
               docValues: Boolean = false,
               sortMissingFirst: Boolean = false,
               sortMissingLast: Boolean = false,
               multiValued: Boolean = false,
               uninvertible: Boolean = true,
               omitNorms: Option[Boolean] = None,
               omitTermFreqAndPositions: Option[Boolean] = None,
               omitPositions: Option[Boolean] = None,
               termVectors: Boolean = false,
               termPositions: Boolean = false,
               termOffsets: Boolean = false,
               termPayloads: Boolean = false,
               required: Boolean = false,
               useDocValuesAsStored: Boolean = true,
               large: Boolean = false): SolrSchema = withInstruction(AddField(
    name = name,
    `type` = `type`.name,
    default = o(default, ""),
    indexed = o(indexed, true),
    stored = o(stored, true),
    docValues = o(docValues, false),
    sortMissingFirst = o(sortMissingFirst, false),
    sortMissingLast = o(sortMissingLast, false),
    multiValued = o(multiValued, false),
    uninvertible = o(uninvertible, true),
    omitNorms = omitNorms,
    omitTermFreqAndPositions = omitTermFreqAndPositions,
    omitPositions = omitPositions,
    termVectors = o(termVectors, false),
    termPositions = o(termPositions, false),
    termOffsets = o(termOffsets, false),
    termPayloads = o(termPayloads, false),
    required = o(required, false),
    useDocValuesAsStored = o(useDocValuesAsStored, true),
    large = o(large, false)
  ))

  def deleteField(name: String): SolrSchema = withInstruction(DeleteField(name))

  def replaceField(name: String,
                   `type`: String,
                   default: String = "",
                   indexed: Boolean = true,
                   stored: Boolean = true,
                   docValues: Boolean = false,
                   sortMissingFirst: Boolean = false,
                   sortMissingLast: Boolean = false,
                   multiValued: Boolean = false,
                   uninvertible: Boolean = false,         // Always set this
                   omitNorms: Option[Boolean] = None,
                   omitTermFreqAndPositions: Option[Boolean] = None,
                   omitPositions: Option[Boolean] = None,
                   termVectors: Boolean = false,
                   termPositions: Boolean = false,
                   termOffsets: Boolean = false,
                   termPayloads: Boolean = false,
                   required: Boolean = false,
                   useDocValuesAsStored: Boolean = true,
                   large: Boolean = false): SolrSchema = withInstruction(ReplaceField(
      name = name,
      `type` = `type`,
      default = o(default, ""),
      indexed = o(indexed, true),
      stored = o(stored, true),
      docValues = o(docValues, false),
      sortMissingFirst = o(sortMissingFirst, false),
      sortMissingLast = o(sortMissingLast, false),
      multiValued = o(multiValued, false),
      uninvertible = o(uninvertible, false),         // Always set this
      omitNorms = omitNorms,
      omitTermFreqAndPositions = omitTermFreqAndPositions,
      omitPositions = omitPositions,
      termVectors = o(termVectors, false),
      termPositions = o(termPositions, false),
      termOffsets = o(termOffsets, false),
      termPayloads = o(termPayloads, false),
      required = o(required, false),
      useDocValuesAsStored = o(useDocValuesAsStored, true),
      large = o(large, false)
    )
  )

  def addCopyField(source: String, dest: String, maxChars: Int = -1): SolrSchema = {
    withInstruction(AddCopyField(source, dest, o(maxChars, -1)))
  }

  def deleteCopyField(source: String, dest: String): SolrSchema = withInstruction(DeleteCopyField(source, dest))
}
