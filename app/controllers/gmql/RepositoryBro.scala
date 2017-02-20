package controllers.gmql

import java.util
import javax.inject.Singleton
import javax.ws.rs.core.Response

import gql.services.rest.Orchestrator.{GMQLFile, GMQLFileTypes}
import gql.services.rest.{MetadataBrowserCached, QueryManager, RepositoryBrowser, SchemaBrowser}
import play.api.Logger
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.mvc.{Controller, RequestHeader, Result}
import wrappers.authanticate.AuthenticatedAction

import scala.collection.JavaConverters._
import scala.xml._

// Combinator syntax


/**
  * Created by canakoglu on 4/11/16.
  */
@Singleton
class RepositoryBro extends Controller {

  implicit object NodeFormat1 extends Writes[Node] {
    override def writes(node: Node): JsValue =
      if (node.child.count(_.isInstanceOf[Text]) == 1)
        JsString(node.text)
      else
        JsObject(
          node.child.collect {
            case e: Elem => e.label -> writes(e)
          }
            ++
            (node.attributes.asAttrMap map { case (a: String, b: String) => a -> JsString(b) }).toSeq
        )
  }

  def browseRepositoryMetadata = AuthenticatedAction { request =>
    val username = request.username.getOrElse("")
    val response = new RepositoryBrowser().browseRepositoryMetadata(username)
    val resAsString = ResultUtils.unMarshall(response, false)
    Ok(resAsString).as("text/xml")
  }

  def browseRepositoryMetadataJson = AuthenticatedAction { request =>
    val username = request.username.getOrElse("")
    val response = new RepositoryBrowser().browseRepositoryMetadata(username)
    val resAsString = ResultUtils.unMarshall(response, false)
    val xmlElem = scala.xml.XML.loadString(resAsString)
    //    val res = Json.toJson(response.getEntity)
    Ok(NodeFormat1.writes(xmlElem))

  }

  def browseRepositorySchemas = AuthenticatedAction { request =>
    val username = request.username.getOrElse("")
    val response = new RepositoryBrowser().browseRepositorySchemas(username)
    val resAsString = ResultUtils.unMarshall(response, false)
    Ok(resAsString).as("text/xml")
  }

  def browseRepositoryQueries = AuthenticatedAction { request =>
    val username = request.username.getOrElse("")
    val response = new RepositoryBrowser().browseRepositoryQueries(username)
    val resAsString = ResultUtils.unMarshall(response, false)
    Ok(resAsString).as("text/xml")
  }

  def browseRepositoryAll = AuthenticatedAction { request =>
    val username = request.username.getOrElse("")
    val response = new RepositoryBrowser().browseRepositoryAll(username)
    val resAsString = ResultUtils.unMarshall(response, false)
    Ok(resAsString).as("text/xml")
  }


  def dataSetSchema(dataSetName: String) = AuthenticatedAction { implicit request =>
    val username = request.username.getOrElse("")
    val file = getFile(username, dataSetName, "schema", "schema")
    val result = if (file._2.isDefined) new SchemaBrowser().getAvailableSchemas(file._2.get) else null
    execute(result, file)
  }

  //  def metadataBrowser = new MetadataBrowserBasic
  def metadataBrowser = new MetadataBrowserCached

  def dataSetMeta(dataSetName: String) = AuthenticatedAction { implicit request =>
    val username = request.username.getOrElse("")
    val file = getFile(username, dataSetName, "metadata", "meta")
    val result = if (file._2.isDefined) metadataBrowser.browseResourceFile(file._2.get) else null
    execute(result, file)
  }

  def dataSetMetaAttribute(dataSetName: String, attributeName: String) = AuthenticatedAction { implicit request =>
    val username = request.username.getOrElse("")
    val file = getFile(username, dataSetName, "metadata", "meta")
    val result = if (file._2.isDefined) metadataBrowser.browseAttribute(file._2.get, attributeName) else null
    execute(result, file)
  }

  def dataSetMetaAttributeValue(dataSetName: String, attributeName: String, attributeValue: String) = AuthenticatedAction { implicit request =>
    val username = request.username.getOrElse("")
    val file = getFile(username, dataSetName, "metadata", "meta")
    val result = if (file._2.isDefined) metadataBrowser.browseAttributeValue(file._2.get, attributeName, attributeValue) else null
    execute(result, file)
  }

  def browseId(dataSetName: String, id: String) = AuthenticatedAction { implicit request =>
    val username = request.username.getOrElse("")
    val file = getFile(username, dataSetName, "metadata", "meta")
    val result = if (file._2.isDefined) metadataBrowser.browseId(file._2.get, id) else null
    execute(result, file)
  }

  //  http://reactive.xploregroup.be/blog/13/Play-JSON-in-Scala-intro-and-beyond-the-basics


  //  case class Name(name: String)
  //
  //  implicit val nameReader: Reads[Name] = (JsPath \ "name").read[String].map(Name)


  //  implicit val valueReader: Reads[Value] = (JsPath \ "name").read[String].map(Value)
  //  implicit val attributeReads: Reads[Attribute] = ((JsPath \ "name").read[String] and (JsPath \ "valueList").read[Seq[Value]]) (Attribute.apply _)
  //  implicit val attributeListReader: Reads[AttributeList] = (JsPath).read[Seq[Attribute]].map(AttributeList)

  case class Attribute(name: String, values: Seq[String])

  object Attribute {
    implicit val attributeReads: Reads[Attribute] = Json.reads[Attribute]
    implicit val writer: Writes[Attribute] = Json.writes[Attribute]

  }

  case class AttributeList(attributes: Seq[Attribute])

  object AttributeList {
    implicit val attributeListReader = Json.reads[AttributeList]
  }


  //  {
  //    "attributes":[
  //    {
  //      "name" : "ann_type1",
  //      "values":["enhancer"]
  //    },
  //    {
  //      "name":"ann_type2",
  //      "values":["enhancer1", "enhancer2"]
  //    },
  //    {
  //      "name":"ann_type3",
  //      "values":[]
  //    }
  //    ]
  //  }


  def getFilteredSamples(dataSetName: String) = AuthenticatedAction(parse.json) { implicit request =>
    val username = request.username.getOrElse("")
    val file = getFile(username, dataSetName, "metadata", "meta")

    //this part is not used, the other method is used
    val attributes2 = new util.ArrayList[String]
    val values2 = new util.ArrayList[String]
    val numbers2 = new util.ArrayList[Integer]

    val placeResult = request.body.validate[AttributeList]
    placeResult.fold(
      errors => {
        Logger.debug(request.body.toString())
        Logger.error(errors.toString())
        BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors)))
      },
      attributeList => {
        Ok(Json.obj("status" -> "OK", "message" -> ("Place '" + attributeList.toString() + "' saved.")))
      }
    )


    val atts = (request.body \ "attributes")
    val attributes = (atts \\ "name").map(_.as[String])
    val valuesMatrix = (atts \\ "values").map(_.as[List[String]])
    val values = valuesMatrix.flatten
    val numbers: Seq[Int] = valuesMatrix.map(_.size)

    val result = if (file._2.isDefined) metadataBrowser.filtermanyExperiments(file._2.get, attributes.asJava, values.asJava, numbers.asJava.asInstanceOf[util.List[Integer]]) else null
    execute(result, file)
  }


  case class Query(name: String, value: Option[String]=None) {
    def getXml =
      <query>
        <name>{name}</name>
        {if(value.isDefined) <value>{value}</value>}
      </query>
  }


  object Query {
    implicit val writer = Json.writes[Query]
  }

  case class Queries(queries: Seq[Query]) {
    def getXml =  <queries>{queries.map(x => x.getXml)}</queries>
  }

  object Queries {
    implicit val writer = Json.writes[Queries]
  }

  def getQueries() = AuthenticatedAction { implicit request =>
    val username = request.username.getOrElse("")
    val response = new RepositoryBrowser().browseRepositoryQueries(username)
    ResultUtils.renderJaxb(response)
    val tree = response.getEntity.asInstanceOf[GMQLFile]



    val list = getAllNamesFromTree(tree).map(name => Query(name.substring(0, name.lastIndexOf("." + GMQLFileTypes.QUERY.getExtension))))
    val queries = Queries(list)
    render {
      case Accepts.Json() => Ok(Json.toJson(queries))
      case Accepts.Xml() => Ok(queries.getXml)
    }
  }


  /**
    * returns all the file names in the tree
    *
    * @param tree
    * @return
    */
  def getAllNamesFromTree(tree: GMQLFile): Seq[String] = {
    if (tree.isDirectory)
      tree.getChildren.asScala.flatMap(file => getAllNamesFromTree(file))
    else
      Seq(tree.getFilename)
  }


  def getQuery(queryName: String) = AuthenticatedAction { implicit request =>
    val username = request.username.getOrElse("")
    val file = getFile(username, queryName, "queries", "gmql")

    file._1 match {
      case "error_no_result" => BadRequest("Unknown data set")
      case "error_multi_result" => BadRequest("Multiple data set")
      case "found" =>
        val response = new QueryManager().readQuery(username, file._2.get)
        val query = Query(queryName,Some(response.getEntity.asInstanceOf[String]))
        render {
          case Accepts.Json() => Ok(Json.toJson(query))
          case Accepts.Xml() => Ok(query.getXml)
        }
    }



  }


  //  UTIL FUNCTIONS

  def execute(result: Response, file: (String, Option[String]))(implicit request: RequestHeader): Result = {
    Logger.debug("execute->result: " + result)
    Logger.debug("execute->result->getEntity: " + result.getEntity)
    file._1 match {
      case "error_no_result" => BadRequest("Unknown data set")
      case "error_multi_result" => BadRequest("Multiple data set")
      case "found" => ResultUtils.renderJaxb(result)
    }
  }

  /**
    *
    * @param username    username from token
    * @param dataSetName dataSetName as input
    * @param folder      the folder name in between
    * @param extension   extension of the file
    * @return the list of the file path
    */
  def getFile(username: String, dataSetName: String, folder: String, extension: String) = {
    val res = new RepositoryBrowser().browseRepositoryAll(username)
    val gmqlFile = res.getEntity.asInstanceOf[GMQLFile]
    Logger.warn(gmqlFile.toString)
    //    var key: List[String] = null
    var result: List[String] = null
    if (dataSetName.startsWith("public.")) {
      val fileName = dataSetName.substring("public.".size)
      result = ResultUtils.findFileKey(gmqlFile, List(Some("public"), Some(folder), Some(s"$fileName.$extension")))
    } else {
      result = ResultUtils.findFileKey(gmqlFile, List(Some(username), Some(folder), Some(s"$dataSetName.$extension")))
    }


    if (result.isEmpty)
      "error_no_result" -> None
    else if (result.size > 2)
      "error_multi_result" -> None
    else
      "found" -> Some(result.head)
  }
}

