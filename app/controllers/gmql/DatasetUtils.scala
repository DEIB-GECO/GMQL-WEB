package controllers.gmql

import java.io.File

import io.swagger.annotations.ApiModelProperty
import it.polimi.genomics.repository.GMQLRepository
import play.api.libs.json._
import play.api.mvc.BodyParsers.parse
import play.api.mvc.Results._
import utils.GmqlGlobal

import scala.collection.JavaConversions._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.util.Try
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
    * Create or clean temp folder for the dataset.
    *
    * @param username    name of the user
    * @param dataSetName name of the dataset
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
    val schemaFilePath = Path.fromString(tempDirPath + "schema.xml")
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
        Some(temp.name),
        info = Some(getInfo(username, datasetName, temp.name.split("/").last.split("\\.").head)))
    ).sorted
  }

  def getInfo(username: String, datasetName: String) = {
    Try(Info((repository.getDatasetMeta(datasetName, username) ++ repository.getDatasetProfile(datasetName, username)).toList.sorted)).getOrElse(Info())
  }

  def getInfo(username: String, datasetName: String, sampleName: String) = {
    Try(Info(repository.getSampleProfile(datasetName, sampleName, username).toList.sorted)).getOrElse(Info())
  }

}


case class Info(infoList: List[(String, String)] = List.empty) {
  @ApiModelProperty(hidden = true)
  def getXml = <info_list>
    {infoList.map { el =>
      <info><key>{el._1}</key><value>{el._2}</value></info>
    }}
  </info_list>
}

object Info {
  implicit val writerTuple = new Writes[(String, String)] {
    def writes(t: (String, String)) = Json.obj(
      "key" -> t._1,
      "value" -> t._2
    )
  }
  implicit val writer = Json.writes[Info]
}


case class Sample(id: String,
                  name: String,
                  @ApiModelProperty(dataType = "string", required = false) path: Option[String] = None,
                  @ApiModelProperty(dataType = "string", required = false) dataset: Option[String] = None,
                  @ApiModelProperty(dataType = "controllers.gmql.Info", required = false) info: Option[Info] = None) extends Ordered[Sample] {
  def compare(that: Sample): Int = this.name.toLowerCase compare that.name.toLowerCase

  @ApiModelProperty(hidden = true)
  def getXml =
      <sample>
        <id>{id}</id>
        <name>{name}</name>
        {if (path.isDefined) <path>{path.get}</path>}
        {if (dataset.isDefined) <dataset>{dataset.get}</dataset>}
        {if (info.isDefined) <info>{info.get}</info>}
      </sample>
}

object Sample {
  implicit val writer = Json.writes[Sample]
}

case class Dataset(var name: String,
                   @ApiModelProperty(dataType = "string", required = false) owner: Option[String] = None,
                   @ApiModelProperty(dataType = "string", required = false) group: Option[String] = None,
                   @ApiModelProperty(dataType = "List[controllers.gmql.Sample]", required = false) samples: Option[Seq[Sample]] = None,
                   @ApiModelProperty(dataType = "controllers.gmql.Info", required = false) info: Option[Info] = None) extends Ordered[Dataset] {
  def compare(that: Dataset): Int = Ordering.Tuple2[Option[String], String].compare((this.owner, this.name.toLowerCase), (that.owner, that.name.toLowerCase))

  @ApiModelProperty(hidden = true)
  def getXml =
      <dataset>
        <name>{name}</name>
        {if (owner.isDefined) <owner>{owner.get}</owner>}
        {if (group.isDefined) <group>{group.get}</group>}
        {if (info.isDefined) <info>{info.get}</info>}
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
               @ApiModelProperty(dataType = "string", required = false) status: Option[String] = None,
               @ApiModelProperty(dataType = "string", required = false) message: Option[String] = None,
               @ApiModelProperty(dataType = "List[controllers.gmql.Dataset]", required = false) datasets: Option[Seq[Dataset]] = None,
               @ApiModelProperty(dataType = "string", required = false) executionTime: Option[Long] = None) {
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

case class Query(name: String,
                 @ApiModelProperty(dataType = "string", required = false) text: Option[String] = None) {
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
  def compare(that: Attribute): Int = Ordering.Tuple2[String, String].compare((this.key.toLowerCase, this.value.getOrElse(Value("")).text.toLowerCase), (that.key.toLowerCase, that.value.getOrElse(Value("")).text.toLowerCase))

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
  final val swaggerSystemAdmin ="System administration"
}

case class MatrixResult(samples: Seq[Sample],
                        attributes: Seq[Attribute],
                        matrix: Seq[Seq[String]]) {

  @ApiModelProperty(hidden = true)
  def getXml =
      <dataset>
        <samples>{samples.map(_.getXml)}</samples>
        <attributes>{attributes.map(_.getXml)}</attributes>
        <matrix>{matrix}</matrix>
      </dataset>


  @ApiModelProperty(hidden = true)
  def getStream(transposed: Boolean) = {
    var upperLeftTitle = "Attributes"
    var firstColumn = attributes.map(attribute => attribute.key)
    var columnNames = samples.map(sample => sample.name)
    var data = matrix


    if (transposed) {
      upperLeftTitle = "Samples"
      columnNames = attributes.map(attribute => attribute.key)
      firstColumn = samples.map(sample => sample.name)
      data = matrix.transpose
    }


    columnNames = upperLeftTitle :: columnNames.toList


    //imported for seperators
    import MatrixResult._
    val stringBuilder = new StringBuilder()
    columnNames.addString(stringBuilder, cvsSeperator).append(lineSeparator)
    firstColumn.zip(matrix).foreach { zip =>
      stringBuilder.append(zip._1).append(cvsSeperator)
      zip._2.addString(stringBuilder, cvsSeperator).append(lineSeparator)
    }
    stringBuilder.toString()
  }
}

object MatrixResult {
  val cvsSeperator = "\t"
  val lineSeparator = new DSManager().lineSeparator
  implicit val writer = Json.writes[MatrixResult]
}


