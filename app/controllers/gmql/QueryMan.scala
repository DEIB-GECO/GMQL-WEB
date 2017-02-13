package controllers.gmql

import javax.inject.Singleton

import controllers.Validation
import gql.services.rest.QueryManager
import it.polimi.genomics.manager.Exceptions.InvalidGMQLJobException
import it.polimi.genomics.manager.{GMQLExecute, GMQLJob}
import play.api.mvc.Controller
import wrappers.authanticate.AuthenticatedAction

import scala.collection.mutable.ListBuffer

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
    if (!flattenErrorList.isEmpty)
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

  def traceJobV2(jobId: String) = AuthenticatedAction { implicit request =>
    val username = request.username.getOrElse("")
    val response = new QueryManager().traceJobv2(username, jobId)
    //    val resAsString = ResultUtils.unMarshallClass(response, classOf[GMQLJobStatusXML], false)
    //    Ok(resAsString).as("text/xml")
    ResultUtils.renderJaxb(response)
  }

  def getLog(jobId: String) = AuthenticatedAction { implicit request =>
    val username = request.username.getOrElse("")
    val response = new QueryManager().getLog(username, jobId)
    //    val resAsString = ResultUtils.unMarshallClass(response, classOf[JobList], false)
    //    Ok(resAsString).as("text/xml")
    ResultUtils.renderJaxb(response)
  }

  /**
    * In order to stop the job this service should be called.
    * @param jobId the id of the job.
    * @return Ok(HTTP 200) with a message that stops the job if the stop execution done correctly,
    *         Forbidden(HTTP 403) message if otherwise(if the job is not exists or the job id is not related to the user)
    */
  def stopJob(jobId: String) = AuthenticatedAction { implicit request =>
    val username = request.username.getOrElse("")
    val server = GMQLExecute()
    try {
      val job: GMQLJob = server.getGMQLJob(username, jobId)
      job.submitHandle.killJob()
      Ok("Job is stopping.")
    } catch {
      case e: InvalidGMQLJobException => Forbidden(e.getMessage)
    }
  }



}
