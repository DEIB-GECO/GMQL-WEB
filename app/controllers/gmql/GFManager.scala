package controllers.gmql

import controllers.gmql.ResultUtils._
import io.swagger.annotations.{ApiImplicitParams, _}
import it.polimi.genomics.repository.GMQLExceptions.GMQLDSNotFound
import it.polimi.genomics.repository.federated.communication.{Downloading, Failed, Pending, NotFound => NotFoundStatus, Success => SuccessStatus}
import javax.inject.Singleton
import play.api.libs.json._
import play.api.mvc._
import play.api.{Logger, Play}
import utils.{GmqlGlobal, ZipEnumerator}
import wrappers.authanticate.{AuthenticatedAction, AuthenticatedRequest}

import scala.concurrent.Future


@Singleton
@Api(value = SwaggerUtils.swaggerFederated, produces = "application/json, application/xml")
class GFManager extends Controller {

  val lineSeparator = sys.props("line.separator")

  @ApiOperation(value = "Import result", notes = "Import a partial result from a given location to this instance")
  @ApiImplicitParams(Array(new ApiImplicitParam(name = "X-AUTH-TOKEN", dataType = "string", paramType = "header", required = true)))
  @ApiResponses(value = Array(new ApiResponse(code = 405, message = "Federated mode is not enabled.")))
  def importResult(jobId: String, dsName: String, locationId: String) = AuthenticatedAction { implicit request =>
    val username = request.username.getOrElse("")


    if (GmqlGlobal.federated_interface.isEmpty) {
      ResultUtils.renderedError(METHOD_NOT_ALLOWED, "Federated mode is not enabled.")
    } else {

      GmqlGlobal.federated_interface.get.importDataset(jobId, dsName, locationId)

      val result = "ok"

      render {
        case Accepts.Xml() => Ok(<result>
          {result}
        </result>)
        case Accepts.Json() => Ok(JsObject(Seq("result" -> JsString(result))))
        case _ => NA
      }
    }
  }

  @ApiOperation(value = "Check import status", notes = "Check the import status")
  @ApiImplicitParams(Array(new ApiImplicitParam(name = "X-AUTH-TOKEN", dataType = "string", paramType = "header", required = true)))
  @ApiResponses(value = Array(new ApiResponse(code = 405, message = "Federated mode is not enabled."),
    new ApiResponse(code = 404, message = "Import not found.")))
  def checkImportStatus(jobId: String, dsName: String) = AuthenticatedAction { implicit request =>
    val username = request.username.getOrElse("")


    if (GmqlGlobal.federated_interface.isEmpty) {
      ResultUtils.renderedError(METHOD_NOT_ALLOWED, "Federated mode is not enabled.")
    } else {

      val status = GmqlGlobal.federated_interface.get.checkImportStatus(jobId, dsName)

      def renderStatus(status: String, message: String = "") = render {
        case Accepts.Xml() => Ok(<result>
          {status}
        </result> <message>
          {message}
        </message>)
        case Accepts.Json() => Ok(JsObject(Seq("result" -> JsString(status), "message" -> JsString(message))))
        case _ => NA
      }

      status match {
        case Pending() => renderStatus("Pending");
        case Downloading() => renderStatus("Downloading");
        case Failed(message) => renderStatus("Failed", message);
        case SuccessStatus() => renderStatus("Success");
        case NotFoundStatus() => renderedError(404, "Import not found.")

      }

    }

  }


  @ApiOperation(value = "Download partial result as a zip",
    notes = "Download partial result, stored at this location, as zip file",
    produces = "file")
  @ApiImplicitParams(Array(new ApiImplicitParam(name = "X-AUTH-TOKEN", dataType = "string", paramType = "header", required = true)))
  @ApiResponses(value = Array(
    new ApiResponse(code = 401, message = "User is not authenticated"),
    new ApiResponse(code = 403, message = "Public datasets cannot be downloaded by user"),
    new ApiResponse(code = 404, message = "Paertial result not found")))
  def zip(jobId: String, dsName: String) = AuthenticatedAction { implicit request =>

    if (GmqlGlobal.federated_interface.isEmpty) {
      ResultUtils.renderedError(METHOD_NOT_ALLOWED, "Federated mode is not enabled.")
    }

    import play.api.libs.concurrent.Execution.Implicits.defaultContext
    var username: String = request.username.get

    var resourceId = jobId + "." + dsName

    Logger.info(request.username.get)
    Logger.info(resourceId)

    try {

      val filesList: List[String] = GmqlGlobal.federated_interface.get.listPartialResultFiles(jobId, dsName)
      Logger.debug("partialFiles" + filesList)

      val sources = filesList.flatMap { fileName =>
        lazy val stream = GmqlGlobal.federated_interface.get.fileStream(jobId, dsName, fileName)
        List(ZipEnumerator.Source(s"$resourceId/${fileName}", { () => Future(Some(stream)) })
        )
      }

      Ok.chunked(ZipEnumerator(sources))(play.api.http.Writeable.wBytes).withHeaders(
        CONTENT_TYPE -> "application/zip",
        CONTENT_DISPOSITION -> s"attachment; filename=$resourceId.zip"
      )

    } catch {
      case _: GMQLDSNotFound => renderedError(NOT_FOUND, s"Resource not found: $resourceId")
    }
  }


  @ApiOperation(value = "Delete partial result", notes = "Delete a partial result stored in this instance")
  @ApiImplicitParams(Array(new ApiImplicitParam(name = "X-AUTH-TOKEN", dataType = "string", paramType = "header", required = true)))
  @ApiResponses(value = Array(new ApiResponse(code = 405, message = "Federated mode is not enabled.")))
  def delete(jobId: String, dsName: String) = AuthenticatedAction { implicit request =>
    val username = request.username.getOrElse("")


    if (GmqlGlobal.federated_interface.isEmpty) {
      ResultUtils.renderedError(METHOD_NOT_ALLOWED, "Federated mode is not enabled.")
    } else {

      GmqlGlobal.federated_interface.get.deletePartialResult(jobId, dsName)

      val result = "ok"

      render {
        case Accepts.Xml() => Ok(<result>
          {result}
        </result>)
        case Accepts.Json() => Ok(JsObject(Seq("result" -> JsString(result))))
        case _ => NA
      }
    }
  }


}