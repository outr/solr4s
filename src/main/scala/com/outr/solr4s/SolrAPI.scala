package com.outr.solr4s

import io.circe.{Json, Printer}
import io.circe.parser._
import io.youi.client.HttpClient
import io.youi.client.intercept.Interceptor
import io.youi.http.content.{Content, StringContent}
import io.youi.http.{HttpRequest, HttpResponse}
import io.youi.net._
import profig.JsonUtil

import scala.concurrent.{ExecutionContext, Future}

class SolrAPI(solr: SolrClient) extends Interceptor {
  private[solr4s] lazy val client = HttpClient
    .url(solr.url)
    .noFailOnHttpStatus
    .dropNullValuesInJson(true)
    .interceptor(this)

  lazy val collections: SolrCollections = new SolrCollections(this)

  def collection(name: String): SolrCollection = new SolrCollection(name, this)

  override def before(request: HttpRequest): Future[HttpRequest] = Future.successful(request)

  override def after(request: HttpRequest, response: HttpResponse): Future[HttpResponse] = {
    request.content.collect {
      case StringContent(value, contentType, _) if contentType == ContentType.`application/json` => {
        parse(value) match {
          case Left(pf) => throw new RuntimeException(s"Parsing Failure for $value (${pf.message})", pf.underlying)
          case Right(j) => j
        }
      }
    } match {
      case Some(json) if response.content.exists(_.contentType == ContentType.`application/json`) => {
        val responseJson = parse(response.content.get.asString) match {
          case Left(pf) => throw new RuntimeException(s"Parsing Failure (${pf.message})", pf.underlying)
          case Right(j) => j
        }
        Future.successful(
          response.withContent(
            Content.string(responseJson
              .deepMerge(Json.obj("originalRequest" -> json))
              .pretty(Printer.spaces2), ContentType.`application/json`)
          )
        )
      }
      case None => Future.successful(response)
    }
  }
}

class SolrCollections(api: SolrAPI) {
  def list()(implicit ec: ExecutionContext): Future[CollectionsList] = api
    .client
    .path(path"/solr/admin/collections")
    .params("action" -> "LIST", "wt" -> "json")
    .call[CollectionsList]
}

class SolrCollection(val collectionName: String, api: SolrAPI) extends UpdateInterface {
  lazy val admin: SolrCollectionAdmin = new SolrCollectionAdmin(collectionName, api)

  override protected val updateClient: HttpClient = api.client.path(Path.parse(s"/solr/$collectionName/update"))

  override def instructions: List[SolrUpdateInstruction] = Nil

  override def withInstruction(instruction: SolrUpdateInstruction): UpdateInterface = {
    SolrUpdateCommand(updateClient, List(instruction))
  }
}

trait UpdateInterface {
  protected def updateClient: HttpClient
  def instructions: List[SolrUpdateInstruction]
  def withInstruction(instruction: SolrUpdateInstruction): UpdateInterface
  def execute()(implicit ec: ExecutionContext): Future[GeneralResponse] = updateClient
    .restful[Json, GeneralResponse](Json.obj(instructions.map(i => i.key -> i.value): _*))

  def add(doc: Json,
          commitWithin: Option[Long] = None,
          overwrite: Option[Boolean] = None): UpdateInterface = {
    withInstruction(DocumentAdd(doc, commitWithin, overwrite))
  }
  def commit(): UpdateInterface = withInstruction(CommitInstruction)
  def optimize(waitSearcher: Boolean = false): UpdateInterface = withInstruction(OptimizeInstruction(waitSearcher))
  def delete(id: Option[String] = None, query: Option[String] = None): UpdateInterface = {
    withInstruction(DeleteInstruction(id, query))
  }
}

case class SolrUpdateCommand(updateClient: HttpClient,
                             instructions: List[SolrUpdateInstruction] = Nil) extends UpdateInterface {
  override def withInstruction(instruction: SolrUpdateInstruction): UpdateInterface = {
    copy(instructions = instructions ::: List(instruction))
  }
}

trait SolrUpdateInstruction {
  def key: String
  def value: Json
}

case class DocumentAdd(doc: Json,
                       commitWithin: Option[Long] = None,
                       overwrite: Option[Boolean] = None) extends SolrUpdateInstruction {
  override def key: String = "add"
  override def value: Json = JsonUtil.toJson(this)
}

object CommitInstruction extends SolrUpdateInstruction {
  override def key: String = "commit"
  override def value: Json = Json.obj()
}

case class OptimizeInstruction(waitSearcher: Boolean) extends SolrUpdateInstruction {
  override def key: String = "optimize"
  override def value: Json = JsonUtil.toJson(this)
}

case class DeleteInstruction(id: Option[String], query: Option[String]) extends SolrUpdateInstruction {
  override def key: String = "delete"
  override def value: Json = JsonUtil.toJson(this)
}

class SolrCollectionAdmin(val collectionName: String, api: SolrAPI) {
  lazy val schema: SolrSchema = SolrSchema(this, api)

  def create(routerName: String = "",
             numShards: Int = -1,
             shards: List[String] = Nil,
             replicationFactor: Int = 1,
             maxShardsPerNode: Int = 1,
             createNodeSet: List[String] = Nil,
             createNodeSetShuffle: Boolean = true,
             collectionConfigName: String = "",
             routerField: String = "",
             propertyName: String = "",
             autoAddReplicas: Boolean = false,
             async: String = "",
             rule: String = "",
             snitch: String = "",
             waitForFinalState: Boolean = false)
            (implicit ec: ExecutionContext): Future[GeneralResponse] = {
    api
      .client
      .path(path"/solr/admin/collections")
      .params("action" -> "CREATE", "name" -> collectionName)
      .param("routerName", routerName, "")
      .param("numShards", numShards, -1)
      .param("shards", shards, Nil)
      .param("replicationFactor", replicationFactor, 1)
      .param("maxShardsPerNode", maxShardsPerNode, 1)
      .param("createNodeSet", createNodeSet, Nil)
      .param("createNodeSetShuffle", createNodeSetShuffle, true)
      .param("collectionConfigName", collectionConfigName, "")
      .param("routerField", routerField, "")
      .param("propertyName", propertyName, "")
      .param("autoAddReplicas", autoAddReplicas, false)
      .param("async", async, "")
      .param("rule", rule, "")
      .param("snitch", snitch, "")
      .param("waitForFinalState", waitForFinalState, false)
      .call[GeneralResponse]
  }

  def modify(maxShardsPerNode: Option[Int] = None,
             replicationFactor: Option[Int] = None,
             autoAddReplicas: Option[Boolean] = None,
             collectionConfigName: Option[String] = None,
             rule: Option[String] = None,
             snitch: Option[String] = None)
            (implicit ec: ExecutionContext): Future[GeneralResponse] = api
    .client
    .path(path"/solr/admin/collections")
    .params("action" -> "MODIFYCOLLECTION", "collection" -> collectionName)
    .param("maxShardsPerNode", maxShardsPerNode, None)
    .param("replicationFactor", replicationFactor, None)
    .param("autoAddReplicas", autoAddReplicas, None)
    .param("collectionConfigName", collectionConfigName, None)
    .param("rule", rule, None)
    .param("snitch", snitch, None)
    .call[GeneralResponse]

  def backup(name: String,
             location: String,
             async: String = "",
             repository: String = "")
            (implicit ec: ExecutionContext): Future[GeneralResponse] = api
    .client
    .path(path"/solr/admin/collections")
    .params("action" -> "BACKUP", "name" -> name, "collection" -> collectionName, "location" -> location)
    .param("async", async, "")
    .param("repository", repository, "")
    .call[GeneralResponse]

  def restore(name: String,
              location: String,
              async: String = "",
              repository: String = "")
             (implicit ec: ExecutionContext): Future[GeneralResponse] = api
    .client
    .path(path"/solr/admin/collections")
    .params("action" -> "RESTORE", "name" -> name, "collection" -> collectionName, "location" -> location)
    .param("async", async, "")
    .param("repository", repository, "")
    .call[GeneralResponse]

  def status(requestId: String)(implicit ec: ExecutionContext): Future[GeneralResponse] = api
    .client
    .path(path"/solr/admin/collections")
    .params("action" -> "REQUESTSTATUS", "requestid" -> requestId, "wt" -> "json")
    .call[GeneralResponse]

  def deleteStatus(requestId: String, flush: Boolean = false)
                  (implicit ec: ExecutionContext): Future[GeneralResponse] = api
    .client
    .path(path"/solr/admin/collections")
    .params("action" -> "DELETESTATUS", "requestid" -> requestId, "wt" -> "json")
    .param("flush", flush, false)
    .call[GeneralResponse]

  def delete(shard: String = "", replica: String = "")
            (implicit ec: ExecutionContext): Future[GeneralResponse] = {
    api
      .client
      .path(path"/solr/admin/collections")
      .params("action" -> "DELETE", "name" -> collectionName)
      .param("shard", shard, "")
      .param("replica", replica, "")
      .call[GeneralResponse]
  }
}

case class SolrSchema(collection: SolrCollectionAdmin, api: SolrAPI, request: SchemaRequest = SchemaRequest()) {
  private val client = api.client.path(Path.parse(s"/solr/${collection.collectionName}/schema"))

  private def o[T](value: T, default: T): Option[T] = if (value != default) {
    Some(value)
  } else {
    None
  }

  def info(implicit ec: ExecutionContext): Future[CollectionSchema] = client.call[CollectionSchema]

  def execute()(implicit ec: ExecutionContext): Future[GeneralResponse] = {
    assert(request.nonEmpty, "No schema changes supplied")
    client.restful[SchemaRequest, GeneralResponse](request)
  }

  def addField(name: String,
               `type`: FieldType,
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
               large: Boolean = false): SolrSchema = {
    val list = request.`add-field`.getOrElse(Nil) ::: List(AddField(
      name = name,
      `type` = `type`.name,
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
    ))
    copy(request = request.copy(`add-field` = Some(list)))
  }

  def deleteField(name: String): SolrSchema = {
    val list = request.`delete-field`.getOrElse(Nil) ::: List(DeleteField(name))
    copy(request = request.copy(`delete-field` = Some(list)))
  }

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
                   large: Boolean = false): SolrSchema = {
    val list = request.`replace-field`.getOrElse(Nil) ::: List(ReplaceField(
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
    copy(request = request.copy(`replace-field` = Some(list)))
  }

  def addCopyField(source: String, dest: String, maxChars: Int = -1): SolrSchema = {
    val list = request.`add-copy-field`.getOrElse(Nil) ::: List(AddCopyField(source, dest, o(maxChars, -1)))
    copy(request = request.copy(`add-copy-field` = Some(list)))
  }

  def deleteCopyField(source: String, dest: String): SolrSchema = {
    val list = request.`delete-copy-field`.getOrElse(Nil) ::: List(DeleteCopyField(source, dest))
    copy(request = request.copy(`delete-copy-field` = Some(list)))
  }
}

case class SchemaRequest(`add-field`: Option[List[AddField]] = None,
                         `delete-field`: Option[List[DeleteField]] = None,
                         `replace-field`: Option[List[ReplaceField]] = None,
                         `add-copy-field`: Option[List[AddCopyField]] = None,
                         `delete-copy-field`: Option[List[DeleteCopyField]] = None) {
  def isEmpty: Boolean = `add-field`.isEmpty && `delete-field`.isEmpty && `replace-field`.isEmpty && `add-copy-field`.isEmpty
  def nonEmpty: Boolean = !isEmpty
}
case class AddField(name: String,
                    `type`: String,
                    default: Option[String],
                    indexed: Option[Boolean],
                    stored: Option[Boolean],
                    docValues: Option[Boolean],
                    sortMissingFirst: Option[Boolean],
                    sortMissingLast: Option[Boolean],
                    multiValued: Option[Boolean],
                    uninvertible: Option[Boolean],
                    omitNorms: Option[Boolean],
                    omitTermFreqAndPositions: Option[Boolean],
                    omitPositions: Option[Boolean],
                    termVectors: Option[Boolean],
                    termPositions: Option[Boolean],
                    termOffsets: Option[Boolean],
                    termPayloads: Option[Boolean],
                    required: Option[Boolean],
                    useDocValuesAsStored: Option[Boolean],
                    large: Option[Boolean])
case class DeleteField(name: String)
case class ReplaceField(name: String,
                        `type`: String,
                        default: Option[String],
                        indexed: Option[Boolean],
                        stored: Option[Boolean],
                        docValues: Option[Boolean],
                        sortMissingFirst: Option[Boolean],
                        sortMissingLast: Option[Boolean],
                        multiValued: Option[Boolean],
                        uninvertible: Option[Boolean],
                        omitNorms: Option[Boolean],
                        omitTermFreqAndPositions: Option[Boolean],
                        omitPositions: Option[Boolean],
                        termVectors: Option[Boolean],
                        termPositions: Option[Boolean],
                        termOffsets: Option[Boolean],
                        termPayloads: Option[Boolean],
                        required: Option[Boolean],
                        useDocValuesAsStored: Option[Boolean],
                        large: Option[Boolean])
case class AddCopyField(source: String, dest: String, maxChars: Option[Int])
case class DeleteCopyField(source: String, dest: String)

case class CollectionsList(responseHeader: ResponseHeader, collections: List[String])

case class GeneralResponse(responseHeader: Option[ResponseHeader],
                           status: Option[ResponseStatus],
                           success: Map[String, ResponseSuccess] = Map.empty,
                           warning: Option[String],
                           exception: Option[ResponseException],
                           error: Option[ResponseError],
                           originalRequest: Option[Json]) {
  def isSuccess: Boolean = exception.isEmpty && error.isEmpty
}

case class ResponseHeader(status: Int, QTime: Int, rf: Option[Int])

case class ResponseStatus(state: String, msg: String)

case class ResponseSuccess(responseHeader: ResponseHeader, core: Option[String])

case class ResponseException(msg: String, rspCode: Int)

case class ResponseError(metadata: List[String], msg: String, code: Int)

case class CollectionSchemaResponse(responseHeader: ResponseHeader, schema: CollectionSchema)

case class CollectionSchema(name: String,
                            version: Double,
                            uniqueKey: String,
                            fieldTypes: List[CollectionFieldType],
                            fields: List[CollectionField],
                            dynamicFields: List[CollectionDynamicField],
                            copyFields: List[CollectionCopyField])

case class CollectionFieldType(name: String, `class`: String)

case class CollectionField(name: String, `type`: String, indexed: Boolean, stored: Boolean, multiValued: Boolean = false)

case class CollectionDynamicField(name: String, `type`: String, multiValued: Boolean = false)

case class CollectionCopyField(source: String, dest: String)

case class FieldType(name: String)

object FieldType {
  lazy val AlphaOnlySort = FieldType("alphaOnlySort")
  lazy val AncestorPath = FieldType("ancestor_path")
  lazy val BBox = FieldType("bbox")
  lazy val Binary = FieldType("binary")
  lazy val Boolean = FieldType("boolean")
  lazy val Currency = FieldType("currency")
  lazy val DescendentPath = FieldType("descendent_path")
  lazy val Ignored = FieldType("ignored")
  lazy val Location = FieldType("location")
  lazy val LocationRecursivePrefixTree = FieldType("location_rpt")
  lazy val Lowercase = FieldType("lowercase")
  lazy val ManagedEnglish = FieldType("managed_en")
  lazy val Payloads = FieldType("payloads")
  lazy val DatePoint = FieldType("pdate")
  lazy val DatePoints = FieldType("pdates")
  lazy val DoublePoint = FieldType("pdouble")
  lazy val DoublePoints = FieldType("pdoubles")
  lazy val FloatPoint = FieldType("pfloat")
  lazy val FloatPoints = FieldType("pfloats")
  lazy val Phonetic = FieldType("phonetic")
  lazy val IntPoint = FieldType("pint")
  lazy val IntPoints = FieldType("pints")
  lazy val LongPoint = FieldType("plong")
  lazy val LongPoints = FieldType("plongs")
  lazy val Point = FieldType("point")
  lazy val PreAnalyzed = FieldType("preanalyzed")
  lazy val Random = FieldType("random")
  lazy val String = FieldType("string")
  lazy val TextArabic = FieldType("text_ar")
  lazy val TextBulgarian = FieldType("text_bg")
  lazy val TextCatalan = FieldType("text_ca")
  lazy val TextCJK = FieldType("text_cjk")
  lazy val TextCKB = FieldType("text_ckb")
  lazy val TextCzech = FieldType("text_cz")
  lazy val TextDanish = FieldType("text_da")
  lazy val TextGerman = FieldType("text_de")
  lazy val TextGreek = FieldType("text_el")
  lazy val TextEnglish = FieldType("text_en")
  lazy val TextEnglishSplitting = FieldType("text_en_splitting")
  lazy val TextEnglishSplittingTight = FieldType("text_en_splitting_tight")
  lazy val TextSpanish = FieldType("text_es")
  lazy val TextBasque = FieldType("text_eu")
  lazy val TextPersion = FieldType("text_fa")
  lazy val TextFinnish = FieldType("text_fi")
  lazy val TextFrench = FieldType("text_fr")
  lazy val TextIrish = FieldType("text_ga")
  lazy val TextGenericSort = FieldType("text_gen_sort")
  lazy val TextGeneral = FieldType("text_general")
  lazy val TextGeneralReversed = FieldType("text_general_rev")
  lazy val TextGalician = FieldType("text_gl")
  lazy val TextHindi = FieldType("text_hi")
  lazy val TextHungarian = FieldType("text_hu")
  lazy val TextArmenian = FieldType("text_hy")
  lazy val TextIndonesian = FieldType("text_id")
  lazy val TextItalian = FieldType("text_it")
  lazy val TextJapanese = FieldType("text_ja")
  lazy val TextKorean = FieldType("text_ko")
  lazy val TextLatvian = FieldType("text_lv")
  lazy val TextDutch = FieldType("text_nl")
  lazy val TextNorwegian = FieldType("text_no")
  lazy val TextPortuguese = FieldType("text_pt")
  lazy val TextRomanian = FieldType("text_ru")
  lazy val TextSwedish = FieldType("text_sv")
  lazy val TextThai = FieldType("text_th")
  lazy val TextTurkish = FieldType("text_tr")
  lazy val TextWhitespace = FieldType("text_ws")
}