package controllers.gmql

import javax.inject.Singleton

import controllers.gmql.ResultUtils.{NA, renderedError}
import io.swagger.annotations.{ApiImplicitParams, ApiOperation, _}
import it.polimi.genomics.core.GDMSUserClass.GDMSUserClass
import it.polimi.genomics.core._
import it.polimi.genomics.core.exception.{GMQLDagException, UserExceedsQuota}
import it.polimi.genomics.core.{BinSize, GMQLSchemaFormat, GMQLScript, ImplementationPlatform}
import it.polimi.genomics.manager.Exceptions.{InvalidGMQLJobException, NoJobsFoundException}
import it.polimi.genomics.manager.Status._
import it.polimi.genomics.manager.{GMQLContext, GMQLExecute, GMQLJob}
import it.polimi.genomics.manager.Launchers.GMQLSparkLauncher
import it.polimi.genomics.repository.GMQLExceptions.{GMQLDSNotFound, GMQLNotValidDatasetNameException}
import org.apache.spark.SparkContext
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.Controller
import wrappers.authanticate.AuthenticatedAction

import scala.collection.JavaConversions._


/**
  * Created by canakoglu on 4/11/16.
  */
@Singleton
@Api(value = SwaggerUtils.swaggerQueryManager, produces = "application/json, application/xml")
class QueryMan extends Controller {

  import utils.GmqlGlobal._

  @ApiOperation(value = "Execute the query",
    notes = "Execute query and for the result user needs to check trace the job.",
    consumes = "text/plain"
  )
  @ApiImplicitParams(Array(new ApiImplicitParam(
    name = "body",
    dataType = "string", paramType = "body"
  ), new ApiImplicitParam(name = "X-AUTH-TOKEN", dataType = "string", paramType = "header", required = true)))
  def runQuery(queryName: String,
               @ApiParam(allowableValues = "tab, gtf") outputType: String) = AuthenticatedAction { implicit request =>
    val username = request.username.getOrElse("")
    val userClass = request.user.get.userType
    val outputFormat = GMQLSchemaFormat.getType(outputType)

    val queryOption = request.body.asText


    try {
      lazy val queryResult = queryOption match {
        case None =>
          None
        case Some(query) =>
          Logger.info("\n" + username + " : " + queryName + "\n" + queryOption + "\n\n\n\n\n")
          val compileResultJob = compileJob(username, query, queryName, outputFormat)
          if (compileResultJob.getJobStatus == COMPILE_FAILED) {
            Some(Job(compileResultJob.jobId, Some(compileResultJob.getJobStatus.toString), Some(compileResultJob.jobOutputMessages.toString())))
          } else {
            val server = GMQLExecute()
            val job = registerJob(username, userClass, query, queryName, outputFormat)
            server.execute(job)
            val datasets = server.getJobDatasets(job.jobId).map(Dataset(_))
            Some(Job(job.jobId, Some(job.status.toString), Some(job.getMessage()), Some(datasets), {
              if (job.getExecutionTime() < 0) None else Some(job.getExecutionTime())
            }))
          }
      }

      render {
        case Accepts.Xml() =>
          if (queryResult.isEmpty)
            ResultUtils.renderedError(NOT_ACCEPTABLE, "Query must be send in the request body")
          else
            Ok(scala.xml.Utility.trim(queryResult.get.getXml))
        case Accepts.Json() =>
          if (queryResult.isEmpty)
            ResultUtils.renderedError(NOT_ACCEPTABLE, "Query must be send in the request body")
          else
            Ok(Json.toJson(queryResult))
        case _ => NA
      }
    } catch {
      case _: UserExceedsQuota => renderedError(BAD_REQUEST, "User quota is exceeded")
      case e: GMQLNotValidDatasetNameException => renderedError(BAD_REQUEST, e.getMessage)
    }
  }

  @ApiOperation(value = "Execute the dag query",
    notes = "Execute dag query and for the result user needs to check trace the job.",
    consumes = "text/plain"
  )
  @ApiImplicitParams(Array(new ApiImplicitParam(
    name = "body",
    dataType = "string", paramType = "body"
  ), new ApiImplicitParam(name = "X-AUTH-TOKEN", dataType = "string", paramType = "header", required = true)))
  def runDag(//queryName: String,
             @ApiParam(allowableValues = "tab, gtf") outputType: String,
             jobId: String = "") = AuthenticatedAction { implicit request =>
    val username = request.username.getOrElse("")
    val outputFormat = GMQLSchemaFormat.getType(outputType)

    val queryOption = request.body.asText


    lazy val queryResult = queryOption match {
      case None =>
        None
      case Some(serializedDAG) =>
        Logger.info("\n" + username + " : " + queryOption + "\n\n\n\n\n")

        val server = GMQLExecute()
        val script = GMQLScript("", "", serializedDAG)
        val userClass = request.user.get.userType
        val gmqlContext = GMQLContext(ImplementationPlatform.SPARK, repository, outputFormat, username = username, userClass = userClass, checkQuota = true)
        val job = server.registerDAG(script, gmqlContext)
        server.execute(job)
        val datasets = server.getJobDatasets(job.jobId).map(Dataset(_))
        Some(Job(job.jobId, Some(job.status.toString), Some(job.getMessage()), Some(datasets), {
          if (job.getExecutionTime() < 0) None else Some(job.getExecutionTime())
        }))
    }

    render {
      case Accepts.Xml() =>
        try {
          if (queryResult.isEmpty)
            ResultUtils.renderedError(NOT_ACCEPTABLE, "Query must be send in the request body")
          else
            Ok(scala.xml.Utility.trim(queryResult.get.getXml))
        } catch {
          case e: GMQLDagException => ResultUtils.renderedError(NOT_ACCEPTABLE, "DAG error: " + e.getMessage)
          case e: GMQLDSNotFound => ResultUtils.renderedError(NOT_ACCEPTABLE, "Dataset not found: " + e.getMessage)
        }
      case Accepts.Json() =>
        try {
          if (queryResult.isEmpty)
            ResultUtils.renderedError(NOT_ACCEPTABLE, "Query must be send in the request body")
          else
            Ok(Json.toJson(queryResult))
        } catch {
          case e: GMQLDagException => ResultUtils.renderedError(NOT_ACCEPTABLE, "DAG error: " + e.getMessage)
          case e: GMQLDSNotFound => ResultUtils.renderedError(NOT_ACCEPTABLE, "Dataset not found: " + e.getMessage)
        }
      case _ => NA
    }
  }


  private def registerJob(username: String, userClass: GDMSUserClass, query: String, queryName: String, outputFormat: GMQLSchemaFormat.Value) = {
    val server = GMQLExecute()
    val gmqlScript = GMQLScript(query, queryName)
    val gmqlContext = GMQLContext(ImplementationPlatform.SPARK, repository, outputFormat, username = username, userClass = userClass, checkQuota = true)
    server.registerJob(gmqlScript, gmqlContext, "")
  }

  private def compileJob(username: String, query: String, queryName: String, outputFormat: GMQLSchemaFormat.Value) = {
    val gmqlScript = GMQLScript(query, queryName)
    val gmqlContext = GMQLContext(ImplementationPlatform.SPARK, repository, outputFormat, username = username, checkQuota = false)
    val job: GMQLJob = new GMQLJob(gmqlContext, gmqlScript, gmqlContext.username)
    job.compile()
    job
  }

  @ApiOperation(value = "Compile query",
    notes = "Compile query and for the result user needs to check trace the job.",
    consumes = "text/plain"
  )
  @ApiImplicitParams(Array(new ApiImplicitParam(
    name = "body",
    dataType = "string", paramType = "body"
    //    , examples = new Example(Array(new ExampleProperty(value = "{\n\t\"schema_file\": \"http://www.bioinformatics.deib.polimi.it/canakoglu/guest_data/HG19_ANN.schema\",\n\t\"data_files\": [\n\t\t\"http://www.bioinformatics.deib.polimi.it/canakoglu/guest_data/RefSeqGenesExons_hg19.bed\",\n\t\t\"http://www.bioinformatics.deib.polimi.it/canakoglu/guest_data/RefSeqGenesExons_hg19.bed.meta\",\n\t\t\"http://www.bioinformatics.deib.polimi.it/canakoglu/guest_data/TSS_hg19.bed\",\n\t\t\"http://www.bioinformatics.deib.polimi.it/canakoglu/guest_data/TSS_hg19.bed.meta\",\n\t\t\"http://www.bioinformatics.deib.polimi.it/canakoglu/guest_data/VistaEnhancers_hg19.bed\",\n\t\t\"http://www.bioinformatics.deib.polimi.it/canakoglu/guest_data/VistaEnhancers_hg19.bed.meta\"\n\t]\n}")))
  ), new ApiImplicitParam(name = "X-AUTH-TOKEN", dataType = "string", paramType = "header", required = true)))
  def compileQuery() = AuthenticatedAction { implicit request =>
    val username = request.username.getOrElse("")
    val queryOption = request.body.asText
    val outputFormat = GMQLSchemaFormat.TAB
    val queryName = "only_compile"

    lazy val queryResult = queryOption match {
      case None =>
        Logger.error("Query must be send in the request body" + "\n" + username + " : " + queryName + "\n" + queryOption + "\n\n\n\n\n")
        None
      case Some(query) =>
        val job = compileJob(username, query, queryName, outputFormat)
        Some(Job(job.jobId, Some(job.getJobStatus.toString), Some(job.jobOutputMessages.toString())))
    }

    render {
      case Accepts.Xml() =>
        if (queryResult.isEmpty)
          ResultUtils.renderedError(NOT_ACCEPTABLE, "Query must be send in the request body")
        else
          Ok(scala.xml.Utility.trim(queryResult.get.getXml))
      case Accepts.Json() =>
        if (queryResult.isEmpty)
          ResultUtils.renderedError(NOT_ACCEPTABLE, "Query must be send in the request body")
        else
          Ok(Json.toJson(queryResult))
      case _ => NA
    }
  }

  @ApiOperation(value = "Get the jobs", notes = "Get the list of the jobs of the current user")
  @ApiImplicitParams(Array(new ApiImplicitParam(name = "X-AUTH-TOKEN", dataType = "string", paramType = "header", required = true)))
  def getJobs = AuthenticatedAction { implicit request =>
    val username = request.username.getOrElse("")

    lazy val jobs: JobList = {
      try {
        JobList(GMQLExecute().getUserJobs(username).map(Job(_)))
      } catch {
        case _: NoJobsFoundException => JobList(List.empty)
      }
    }

    render {
      case Accepts.Xml() => Ok(jobs.getXml)
      case Accepts.Json() => Ok(Json.toJson(jobs))
      case _ => NA
    }
  }

  @ApiOperation(value = "Trace the job", notes = "Trace the job with the id")
  @ApiImplicitParams(Array(new ApiImplicitParam(name = "X-AUTH-TOKEN", dataType = "string", paramType = "header", required = true)))
  def traceJob(jobId: String) = AuthenticatedAction { implicit request =>
    val username = request.username.getOrElse("")

    lazy val server = GMQLExecute();
    //        String user = sc.getUserPrincipal().getName();
    lazy val job = server.getGMQLJob(username, jobId)

    lazy val datasets = server.getJobDatasets(jobId).map(Dataset(_))
    lazy val jobResult = Job(job.jobId, Some(job.status.toString), Some(job.getMessage()), Some(datasets), {
      if (job.getExecutionTime() < 0) None else Some(job.getExecutionTime())
    })

    render {
      case Accepts.Xml() => Ok(jobResult.getXml)
      case Accepts.Json() => Ok(Json.toJson(jobResult))
      case _ => NA
    }


  }

  @ApiOperation(value = "Get the log of the job", notes = "Returns the log of the job with job id")
  @ApiImplicitParams(Array(new ApiImplicitParam(name = "X-AUTH-TOKEN", dataType = "string", paramType = "header", required = true)))
  def getLog(jobId: String) = AuthenticatedAction { implicit request =>
    val username = request.username.getOrElse("")
    lazy val jobLog = Log(GMQLExecute().getGMQLJob(username, jobId).getLog)

    render {
      case Accepts.Xml() => Ok(jobLog.getXml)
      case Accepts.Json() => Ok(Json.toJson(jobLog))
      case _ => NA
    }
  }


  /**
    * In order to stop the job this service should be called.
    *
    * @param jobId the id of the job.
    * @return Ok(HTTP 200) with a message that stops the job if the stop execution done correctly,
    *         Forbidden(HTTP 403) message if otherwise(if the job is not exists or the job id is not related to the user)
    */
  @ApiOperation(value = "Stop the job", notes = "Stops the job with the jobs id")
  @ApiImplicitParams(Array(new ApiImplicitParam(name = "X-AUTH-TOKEN", dataType = "string", paramType = "header", required = true)))
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
