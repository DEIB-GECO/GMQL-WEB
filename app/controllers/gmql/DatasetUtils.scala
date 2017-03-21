package controllers.gmql

import java.io.File

import io.swagger.annotations.ApiModelProperty
import it.polimi.genomics.repository.GMQLRepository
import play.api.libs.json._
import play.api.mvc.BodyParsers.parse
import play.api.mvc.Results._
import utils.GmqlGlobal

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.xml.Elem
import scalax.file.Path


//TODO REMOVE
object DatasetUtils {
  val repository: GMQLRepository = GmqlGlobal.repository
  val ut = GmqlGlobal.ut

  //  //TODO check the folder definition
  //  def checkOrCreateRegionsDirectory(user: String): String = {
  //    val regionsUserFolderPath: String = ut.GMQLHOME + File.separator + "data" + File.separator + user + File.separator + "regions"
  //    val d: File = new File(regionsUserFolderPath)
  //    if (!d.exists) d.mkdir
  //    regionsUserFolderPath
  //  }
  /**
    * Create or clean temp folder for the data set.
    *
    * @param username    name of the user
    * @param dataSetName name of the data set
    * @return full path to the temporary directory
    */
  def createEmptyTempDirectory(username: String, dataSetName: String) = {
    val tempDirPath = ut.getTempDir(username) + File.separator + "upload" + File.separator + dataSetName + File.separator
    //delete folder if exists
    val regionsDirPath = Path.fromString(tempDirPath)
    regionsDirPath.deleteRecursively(force = true, continueOnFailure = true)
    regionsDirPath.doCreateParents()
    regionsDirPath.doCreateDirectory()
    //delete schema if exists
    val schemaFilePath = Path.fromString(tempDirPath + ".schema")
    schemaFilePath.deleteIfExists(force = true)
    tempDirPath
  }

  def deleteTemp(directoryPath: String) = {
    //delete folder if exists
    val regionsDirPath = Path.fromString(directoryPath)
    regionsDirPath.deleteRecursively(force = true, continueOnFailure = true)
  }

  def validateJson[A: Reads] = parse.json.validate(
    _.validate[A].asEither.left.map(e => BadRequest(JsError.toJson(e)))
  )

  def getSamples(username: String, datasetName: String): Seq[Sample] = {
    repository.listDSSamples(datasetName, username).map(temp =>
      Sample(temp.ID,
        //TODO check the correctness
        temp.name.split("/").last.split("\\.").head, /*name is the last part of the file*/
        Some(temp.name))
    ).sorted
  }

}


case class Sample(id: String,
                  name: String,
                  @ApiModelProperty(dataType = "String", required = false) path: Option[String] = None,
                  @ApiModelProperty(dataType = "String", required = false) dataset: Option[String] = None) extends Ordered[Sample] {
  def compare(that: Sample): Int = this.name.toLowerCase compare that.name.toLowerCase

  @ApiModelProperty(hidden = true)
  def getXml =
      <sample>
        <id>{id}</id>
        <name>{name}</name>
        {if (path.isDefined) <path>{path.get}</path>}
        {if (dataset.isDefined) <dataset>{dataset.get}</dataset>}
      </sample>
}

object Sample {
  implicit val writer = Json.writes[Sample]
}

case class Dataset(var name: String,
                   @ApiModelProperty(dataType = "String", required = false) owner: Option[String] = None,
                   @ApiModelProperty(dataType = "String", required = false) group: Option[String] = None,
                   @ApiModelProperty(dataType = "List[controllers.gmql.Sample]", required = false) samples: Option[Seq[Sample]] = None) extends Ordered[Dataset] {
  def compare(that: Dataset): Int = Ordering.Tuple2[Option[String], String].compare((this.owner, this.name.toLowerCase), (that.owner, that.name.toLowerCase))

  @ApiModelProperty(hidden = true)
  def getXml =
      <dataset>
        <name>{name}</name>
        {if (owner.isDefined) <owner>{owner.get}</owner>}
        {if (group.isDefined) <group>{group.get}</group>}
        {if (samples.isDefined) <samples>{samples.get.map(_.getXml)}</samples>}
      </dataset>
}

object Dataset {
  implicit val writer = Json.writes[Dataset]
}

case class Datasets(datasets: Seq[Dataset]) {
  @ApiModelProperty(hidden = true)
  def getXml = <datasets>{datasets.map(_.getXml)}</datasets>
}

object Datasets {
  implicit val writer = Json.writes[Datasets]
}


// job id,
case class Job(id: String,
               @ApiModelProperty(dataType = "String", required = false) status: Option[String] = None,
               @ApiModelProperty(dataType = "String", required = false) message: Option[String] = None,
               @ApiModelProperty(dataType = "List[controllers.gmql.Dataset]", required = false) datasets: Option[Seq[Dataset]] = None,
               @ApiModelProperty(dataType = "String", required = false) executionTime: Option[String] = None) {
  @ApiModelProperty(hidden = true)
  def getXml =
    <job>
      <id>{id}</id>
      {if (status.isDefined) <status>{status.get}</status>}
      {if (message.isDefined) <message>{message.get}</message>}
      {if (datasets.isDefined) datasets.get.map(_.getXml)}
      {if (executionTime.isDefined) <execution_time>{executionTime.get}</execution_time>}
    </job>
}

object Job {
  implicit val writer = Json.writes[Job]
}


// job id,
case class JobList(jobs: Seq[Job]) {
  @ApiModelProperty(hidden = true)
  def getXml =
    <job_list>
       jobs.map(_.getXml)
    </job_list>
}

object JobList {
  implicit val writer = Json.writes[JobList]
}

case class QueryResult(@ApiModelProperty(dataType = "controllers.gmql.Job", required = false) job: Option[Job]) {
  @ApiModelProperty(hidden = true)
  def getXml =
    <query_result>
      {if (job.isDefined) job.get.getXml}
    </query_result>
}

object QueryResult {
  implicit val writer = Json.writes[QueryResult]
}

case class Query(name: String,
                 @ApiModelProperty(dataType = "String", required = false) text: Option[String] = None) {
  @ApiModelProperty(hidden = true)
  def getXml =
        <query>
          <name>{name}</name>
          {if (text.isDefined) <text>{text.get}</text>}
        </query>
}


object Query {
  implicit val writer = Json.writes[Query]
}

case class Queries(queries: Seq[Query]) {
  @ApiModelProperty(hidden = true)
  def getXml =  <queries>{queries.map(x => x.getXml)}</queries>
}

object Queries {
  implicit val writer = Json.writes[Queries]
}


//case class LogLine(logLine: String) {
//  def getXml =  <log_line>{logLine}</log_line>
//}
//
//object LogLine {
//  implicit val writer = Json.writes[LogLine]
//}

case class Log(log: Seq[String]) {
  @ApiModelProperty(hidden = true)
  def getXml =  <log>{log.map(x => <line>x</line>)}</log>
}

object Log {
  implicit val writer = Json.writes[Log]
}


case class Value(text: String,
                 @ApiModelProperty(dataType = "Int", required = false) count: Option[Int] = None) extends Ordered[Value] {
  def compare(that: Value): Int = this.text.toLowerCase compare that.text.toLowerCase

  @ApiModelProperty(hidden = true)
  def getXml: Elem =
    if (count.isEmpty)
      <value>{text}</value>
    else
      <value count={count.get.toString}>{text}</value>
}


object Value {
  implicit val reader = Json.reads[Value]
  implicit val writer = Json.writes[Value]

}

case class Attribute(key: String,
                     @ApiModelProperty(dataType = "controllers.gmql.Value", required = false) value: Option[Value] = None,
                     @ApiModelProperty(dataType = "List[controllers.gmql.Value]", required = false) var values: Option[Seq[Value]] = None,
                     @ApiModelProperty(dataType = "Int", required = false) valueCount: Option[Int] = None,
                     @ApiModelProperty(dataType = "Int", required = false) sampleCount: Option[Int] = None) extends Ordered[Attribute] {
  def compare(that: Attribute): Int = 1 // Ordering.Tuple2[String, Option[Value]].compare((this.key.toLowerCase, this.value), (that.key.toLowerCase, that.value))

  values = values.map(_.sorted)

  // returns all the values, possibly unsorted
  def getAllValues = {
    var result = values.getOrElse(Seq.empty).toList
    if (value.nonEmpty)
      result = value.get :: result
    result
  }

  @ApiModelProperty(hidden = true)
  def getXml =
    <attribute>
      <key>{key}</key>
      {if (value.isDefined) value.get.getXml}
      {if (values.isDefined) <values>{values.get.map(_.getXml)}</values>}
      {if (valueCount.isDefined) valueCount.get}
      {if (sampleCount.isDefined) sampleCount.get}
    </attribute>
}


object Attribute {
  implicit val reader = Json.reads[Attribute]
  implicit val writer = Json.writes[Attribute]

}

case class AttributeList(attributes: Seq[Attribute]) {
  @ApiModelProperty(hidden = true)
  def getXml = <attributes>{attributes.map(_.getXml)}</attributes>
}

object AttributeList {
  implicit val reader = Json.reads[AttributeList]
  implicit val writer = Json.writes[AttributeList]
}

case class UploadResult(imported: Seq[Sample],
                         @ApiModelProperty(dataType = "List[controllers.gmql.Sample]", required = false) autoMetadata: Option[Seq[Sample]] = None,
                         @ApiModelProperty(dataType = "List[controllers.gmql.Sample]", required = false) regionProblem: Option[Seq[Sample]] = None) {
  @ApiModelProperty(hidden = true)
  def getXml =
    <upload_result>
      <imported>{imported.map(_.getXml)}</imported>
      {if (autoMetadata.isDefined) <auto_metadata>{autoMetadata.get.map(_.getXml)}</auto_metadata>}
      {if (regionProblem.isDefined) <region_problem>{regionProblem.get.map(_.getXml)}</region_problem>}
    </upload_result>
}

object UploadResult {
  implicit val writer = Json.writes[UploadResult]
}

object SecurityControllerDefaults {
  final val AUTH_TOKEN_HEADER: String = "X-AUTH-TOKEN"
  final val AUTH_TOKEN_COOKIE: String = "authToken"
  final val AUTH_TOKEN_JSON: String = "authToken"
  final val QUERY_AUTH_TOKEN: String = "authToken"
  final val GUEST_USER: String = "guest_new"
  final val PUBLIC_USER: String = "public"
}

object SwaggerUtils {
  final val swaggerRepository = "Repository"
  final val swaggerMetadata = "Metadata"
  final val swaggerQueryBrowser = "Query Browser"
  final val swaggerQueryManager = "Query Execution"
  final val swaggerSecurityController = "Security controller"
}
