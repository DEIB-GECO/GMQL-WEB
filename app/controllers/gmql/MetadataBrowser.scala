package controllers.gmql

import javax.inject.Singleton

import controllers.gmql.ResultUtils.{NA, renderedError}
import it.polimi.genomics.repository.GMQLExceptions.{GMQLDSNotFound, GMQLSampleNotFound}
import it.polimi.genomics.repository.GMQLRepository
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, Controller}
import utils.GmqlGlobal
import wrappers.authanticate.AuthenticatedAction

import scala.collection.JavaConversions._

/**
  * Created by canakoglu on 4/11/16.
  */
@Singleton
class MetadataBrowser extends Controller {
  val repository: GMQLRepository = GmqlGlobal.repository
  val ut = GmqlGlobal.ut
  val newLine = sys.props("line.separator")

  //  /**
  //    * Browses a metadata file to get the set of unique attributes it contains.
  //    *
  //    * @return
  //    */
  //  def browseResourceFile(fileKey: String) = AuthenticatedAction { request =>
  //    val asd = new DSManager
  //    val response = new MetadataBrowserBasic().browseResourceFile(fileKey)
  //    val resAsString = ResultUtils.unMarshall(response, false)
  //    Ok(resAsString).as("text/xml")
  //  }
  //
  //
  //  def browseAttribute(fileKey: String, attribute:String ) = AuthenticatedAction { request =>
  //    val response = new MetadataBrowserBasic().browseAttribute(fileKey, attribute)
  //    val resAsString = ResultUtils.unMarshall(response, false)
  //    Ok(resAsString).as("text/xml")
  //  }
  //
  //  def browseAttributeValue(fileKey: String, attribute:String ,value:String ) = AuthenticatedAction { request =>
  //    val response = new MetadataBrowserBasic().browseAttributeValue(fileKey, attribute, value)
  //    val resAsString = ResultUtils.unMarshall(response, false)
  //    Ok(resAsString).as("text/xml")
  //  }
  //
  //
  //
  //  def browseId(fileKey: String, id:String ) = AuthenticatedAction { request =>
  //    val response = new MetadataBrowserBasic().browseId(fileKey, id)
  //    val resAsString = ResultUtils.unMarshall(response, false)
  //    Ok(resAsString).as("text/xml")
  //  }
  //
  //  def getAllExperiments(fileKey: String, attribute:String ,value:String ) = AuthenticatedAction { request =>
  //    val response = new MetadataBrowserBasic().getAllExperiments(fileKey, attribute, value)
  //    val resAsString = ResultUtils.unMarshall(response, false)
  //    Ok(resAsString).as("text/xml")
  //  }
  //
  //
  //  //TODO it is empty in the other file
  //  def filterExperiments(fileKey: String, attribute:String ,values: List[String] ) = TODO

  def getSampleMetadata(datasetName: String, sampleName: String) = AuthenticatedAction { implicit request =>
    var username: String = request.username.get
    var dsName = datasetName
    // if public then user name is public and get the correct dataset name
    if (datasetName.startsWith("public.")) {
      username = "public"
      dsName = dsName.substring("public.".length)
    }
    try {
      lazy val result = DatasetMetadata(username, dsName).getSampleMetadata(sampleName: String)
      render {
        case Accepts.Xml() =>
          Ok(scala.xml.Utility.trim(result.getXml))
        case Accepts.Json() =>
          Ok(Json.toJson(result))
        case _ => NA
      }
    } catch {
      case e: GMQLDSNotFound => renderedError(NOT_FOUND, s"Dataset not found ${e.getMessage}")
      case e: GMQLSampleNotFound => renderedError(NOT_FOUND, s"Sample not found ${e.getMessage}")
    }
  }


  def getKeys(datasetName: String) = AuthenticatedAction { implicit request =>
    var username: String = request.username.get
    var dsName = datasetName
    // if public then user name is public and get the correct dataset name
    if (datasetName.startsWith("public.")) {
      username = "public"
      dsName = dsName.substring("public.".length)
    }
    try {
      lazy val result = DatasetMetadata(username, dsName).getAllKeys

      render {
        case Accepts.Xml() => Ok(scala.xml.Utility.trim(result.getXml))
        case Accepts.Json() => Ok(Json.toJson(result))
        case _ => NA
      }
    } catch {
      case e: GMQLDSNotFound => renderedError(NOT_FOUND, s"Dataset not found ${e.getMessage}")
      case e: GMQLSampleNotFound => renderedError(NOT_FOUND, s"Sample not found ${e.getMessage}")
    }
  }

  def getValues(datasetName: String, key: String) = AuthenticatedAction { implicit request =>
    var username: String = request.username.get
    var dsName = datasetName
    // if public then user name is public and get the correct dataset name
    if (datasetName.startsWith("public.")) {
      username = "public"
      dsName = dsName.substring("public.".length)
    }
    try {
      lazy val result = DatasetMetadata(username, dsName).getAllValues(key)

      render {
        case Accepts.Xml() => Ok(scala.xml.Utility.trim(result.getXml))
        case Accepts.Json() => Ok(Json.toJson(result))
        case _ => NA
      }
    } catch {
      case e: GMQLDSNotFound => renderedError(NOT_FOUND, s"Dataset not found ${e.getMessage}")
      case e: GMQLSampleNotFound => renderedError(NOT_FOUND, s"Sample not found ${e.getMessage}")
    }
  }


  def getFilteredDataset(datasetName: String) = AuthenticatedAction(DatasetUtils.validateJson[AttributeList]) { implicit request =>
    var username: String = request.username.get
    var dsName = datasetName
    // if public then user name is public and get the correct dataset name
    if (datasetName.startsWith("public.")) {
      username = "public"
      dsName = dsName.substring("public.".length)
    }
    try {
      val attributeList = request.body
      lazy val dataset = DatasetMetadata(username, dsName).getFilteredDatasets(attributeList)
      render {
        case Accepts.Xml() => Ok(scala.xml.Utility.trim(dataset.getXml))
        case Accepts.Json() => Ok(Json.toJson(dataset))
        case _ => NA
      }
    } catch {
      case e: GMQLDSNotFound => renderedError(NOT_FOUND, s"Dataset not found ${e.getMessage}")
      case e: GMQLSampleNotFound => renderedError(NOT_FOUND, s"Sample not found ${e.getMessage}")
    }
  }


  def getFilteredKeys(datasetName: String) = AuthenticatedAction(DatasetUtils.validateJson[AttributeList]) { implicit request =>
    var username: String = request.username.get
    var dsName = datasetName
    // if public then user name is public and get the correct dataset name
    if (datasetName.startsWith("public.")) {
      username = "public"
      dsName = dsName.substring("public.".length)
    }
    try {
      val attributeList = request.body
      lazy val result = DatasetMetadata(username, dsName).getFilteredKeys(attributeList)
      render {
        case Accepts.Xml() => Ok(scala.xml.Utility.trim(result.getXml))
        case Accepts.Json() => Ok(Json.toJson(result))
        case _ => NA
      }
    } catch {
      case e: GMQLDSNotFound => renderedError(NOT_FOUND, s"Dataset not found ${e.getMessage}")
      case e: GMQLSampleNotFound => renderedError(NOT_FOUND, s"Sample not found ${e.getMessage}")
    }
  }


  def getFilteredValues(datasetName: String, key: String) = AuthenticatedAction(DatasetUtils.validateJson[AttributeList]) { implicit request =>
    var username: String = request.username.get
    var dsName = datasetName
    // if public then user name is public and get the correct dataset name
    if (datasetName.startsWith("public.")) {
      username = "public"
      dsName = dsName.substring("public.".length)
    }
    try {
      val attributeList = request.body
      lazy val result = DatasetMetadata(username, dsName).getFilteredValues(attributeList, key)

      render {
        case Accepts.Xml() => Ok(scala.xml.Utility.trim(result.getXml))
        case Accepts.Json() => Ok(Json.toJson(result))
        case _ => NA
      }
    } catch {
      case e: GMQLDSNotFound => renderedError(NOT_FOUND, s"Dataset not found ${e.getMessage}")
      case e: GMQLSampleNotFound => renderedError(NOT_FOUND, s"Sample not found ${e.getMessage}")
    }
  }
}
