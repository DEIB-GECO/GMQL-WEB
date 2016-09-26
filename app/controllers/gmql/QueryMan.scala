package controllers.gmql

import javax.inject.Singleton

import controllers.Validation
import gql.services.rest.QueryManager
import play.api.mvc.Controller
import wrappers.authanticate.AuthenticatedAction

import scala.collection.mutable.ListBuffer
import scala.util.Try

/**
  * Created by canakoglu on 4/11/16.
  */
@Singleton
class QueryMan extends Controller {

  def readQuery(fileKey: String) = AuthenticatedAction { request =>
    val username = request.username.getOrElse("")
    val response = new QueryManager().readQuery(username, fileKey)
    val resAsString = ResultUtils.unMarshall(response, false)
    Ok(resAsString).as("text/xml")
  }

  def deleteQuery(fileKey: String) = AuthenticatedAction { request =>
    val username = request.username.getOrElse("")
    val response = new QueryManager().deleteQuery(username, fileKey)
    val resAsString = ResultUtils.unMarshall(response, false)
    Ok(resAsString).as("text/xml")
  }


  def saveQueryAs(fileName: String, fileKey: String) = AuthenticatedAction { request =>
    val username = request.username.getOrElse("")
    val errorList = ListBuffer.empty[Option[String]]

    errorList += Validation.validateFilename(fileName)
    val flattenErrorList = errorList.flatten.mkString("\n")
    if(!flattenErrorList.isEmpty)
      BadRequest(flattenErrorList)
    else {
      val query = request.body.asText
      val response = new QueryManager().saveQueryAs(query.getOrElse(""), username, fileName, fileKey)
      ResultUtils.resultHelper(response)
    }
  }

  def runQueryV2File(fileKey: String, gtfOutput: String, execType: String) = AuthenticatedAction { request =>
    val username = request.username.getOrElse("")
    val response = new QueryManager().runQueryV2File(username, fileKey, gtfOutput, execType)
    ResultUtils.resultHelper(response)
  }

  def compileQueryV2File(fileKey: String, execType: String) = AuthenticatedAction { request =>
    val username = request.username.getOrElse("")
    val response = new QueryManager().runCompileV2(username, fileKey, execType)
    ResultUtils.resultHelper(response)
  }


  def getJobsV2 = AuthenticatedAction { implicit request =>
    val username = request.username.getOrElse("")

      val response = new QueryManager().getJobsv2(username)
      //    val resAsString = ResultUtils.unMarshallClass(response, classOf[JobList], false)
      //    Ok(resAsString).as("text/xml")
      ResultUtils.renderJaxb(response)
  }

  def traceJobV2(jobid: String) = AuthenticatedAction { implicit request =>
    val username = request.username.getOrElse("")
    val response = new QueryManager().traceJobv2(username, jobid)
    //    val resAsString = ResultUtils.unMarshallClass(response, classOf[GMQLJobStatusXML], false)
    //    Ok(resAsString).as("text/xml")
    ResultUtils.renderJaxb(response)
  }

  def getLog(jobid: String) = AuthenticatedAction { implicit request =>
    val username = request.username.getOrElse("")
    val response = new QueryManager().getLog(username, jobid)
    //    val resAsString = ResultUtils.unMarshallClass(response, classOf[JobList], false)
    //    Ok(resAsString).as("text/xml")
    ResultUtils.renderJaxb(response)
  }


}
