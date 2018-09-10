package controllers.gmql

import javax.inject.Singleton

import controllers.gmql.ResultUtils.{NA, renderedError}
import io.swagger.annotations.{ApiImplicitParams, _}
import it.polimi.genomics.repository.GMQLExceptions.{GMQLDSNotFound, GMQLSampleNotFound}
import play.api.Logger
import play.api.http.MimeTypes
import play.api.libs.json._
import play.api.mvc.{Accepting, Controller}
import wrappers.authanticate.AuthenticatedAction

/**
  * Created by canakoglu on 4/11/16.
  */
@Singleton
@Api(value = SwaggerUtils.swaggerMetadata, produces = "application/json, application/xml", hidden = true)
class MetadataBrowser extends Controller {
  val newLine = sys.props("line.separator")

  /**
    * get all the metadata of the sample
    *
    * @param datasetName dataset name
    * @param sampleName  sample name
    * @return returns the list of the leys and
    */
  @ApiImplicitParams(Array(new ApiImplicitParam(name = "X-AUTH-TOKEN", dataType = "string", paramType = "header", required = true)))
  def getSampleMetadata(datasetName: String, sampleName: String) = AuthenticatedAction { implicit request =>
    var username: String = request.username.get
    var dsName = datasetName
    // if public[federated] then user name is public[federated] and get the correct dataset name
    if (datasetName.startsWith("public.")) {
      username = "public"
      dsName = dsName.substring("public.".length)
    } else if(datasetName.startsWith("federated.")) {
      username = "federated"
      dsName = dsName.substring("federated.".length)
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

  /**
    * get all the keys of the dataset.
    *
    * @param datasetName
    * @return
    */
  @ApiImplicitParams(Array(new ApiImplicitParam(name = "X-AUTH-TOKEN", dataType = "string", paramType = "header", required = true)))
  def getKeys(datasetName: String) = AuthenticatedAction { implicit request =>
    var username: String = request.username.get
    var dsName = datasetName
    // if public then user name is public and get the correct dataset name
    if (datasetName.startsWith("public.")) {
      username = "public"
      dsName = dsName.substring("public.".length)
    } else if (datasetName.startsWith("federated.")){
      username = "federated"
      dsName = dsName.substring("federated.".length)
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

  @ApiImplicitParams(Array(new ApiImplicitParam(name = "X-AUTH-TOKEN", dataType = "string", paramType = "header", required = true)))
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


  @ApiImplicitParams(Array(new ApiImplicitParam(name = "X-AUTH-TOKEN", dataType = "string", paramType = "header", required = true)))
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

  @ApiImplicitParams(Array(new ApiImplicitParam(name = "X-AUTH-TOKEN", dataType = "string", paramType = "header", required = true)))
  def getFilteredMaxtrix(datasetName: String, transposed:Boolean = false) = AuthenticatedAction(DatasetUtils.validateJson[AttributeList]) { implicit request =>
    var username: String = request.username.get
    var dsName = datasetName
    // if public then user name is public and get the correct dataset name
    if (datasetName.startsWith("public.")) {
      username = "public"
      dsName = dsName.substring("public.".length)
    }
    try {
      val attributeList = request.body
      lazy val matrixResult: MatrixResult = DatasetMetadata(username, dsName).getFilteredMatrix(attributeList)
      lazy val matrixStream = matrixResult.getStream(transposed)
      val Text = Accepting(MimeTypes.TEXT)
      render {
        case Text() =>
          Ok(matrixStream).withHeaders(
            "Content-Type" -> "text/plain",
            "Content-Disposition" -> s"attachment; filename=$dsName.txt"
          )
        case Accepts.Xml() => Ok(scala.xml.Utility.trim(matrixResult.getXml))
        case Accepts.Json() => Ok(Json.toJson(matrixResult))
        case _ =>
          Ok(matrixStream).withHeaders(
            "Content-Type" -> "text/plain",
            "Content-Disposition" -> s"attachment; filename=$dsName.txt"
          )
      }
    } catch {
      case e: GMQLDSNotFound => renderedError(NOT_FOUND, s"Dataset not found ${e.getMessage}")
      case e: GMQLSampleNotFound => renderedError(NOT_FOUND, s"Sample not found ${e.getMessage}")
    }
  }


  //  var temp:Option[Result] = None



  @ApiImplicitParams(Array(new ApiImplicitParam(name = "X-AUTH-TOKEN", dataType = "string", paramType = "header", required = true)))
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
      lazy val result = if (attributeList.attributes.isEmpty) DatasetMetadata(username, dsName).getAllKeys else DatasetMetadata(username, dsName).getFilteredKeys(attributeList)
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


  @ApiImplicitParams(Array(new ApiImplicitParam(name = "X-AUTH-TOKEN", dataType = "string", paramType = "header", required = true)))
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
      lazy val result = if (attributeList.attributes.isEmpty) DatasetMetadata(username, dsName).getAllValues(key) else DatasetMetadata(username, dsName).getFilteredValues(attributeList, key)

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
