package controllers.gmql

import java.io._
import java.net.URL
import java.util
import javax.inject.Singleton

import controllers.gmql.ResultUtils._
import io.swagger.annotations.{ApiImplicitParams, _}
import it.polimi.genomics.core.DataStructures.IRDataSet
import it.polimi.genomics.core.{GNull, _}
import it.polimi.genomics.repository.FSRepository.FS_Utilities
import it.polimi.genomics.repository.GMQLExceptions.{GMQLDSNotFound, GMQLNotValidDatasetNameException, GMQLSampleNotFound}
import it.polimi.genomics.repository._
import it.polimi.genomics.spark.implementation.loaders.CustomParser
import org.xml.sax.SAXException
import play.api.Play.current
import play.api.libs.functional.syntax._
import play.api.libs.iteratee.{Enumeratee, Enumerator}
import play.api.libs.json._
import play.api.mvc._
import play.api.{Logger, Play}
import utils.{VocabularyCount, ZipEnumerator}
import wrappers.authanticate.{AuthenticatedAction, AuthenticatedRequest}

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.sys.process._
import scala.xml.Elem

/**
  * Created by Canakoglu on 15-Mar-16.
  */
@Singleton
@Api(value = SwaggerUtils.swaggerRepository, produces = "application/json, application/xml")
class DSManager extends Controller {

  import utils.GmqlGlobal._

  val lineSeparator = sys.props("line.separator")


  /**
    * Return the merged list of the dataset of the user and <i>public</i> user.
    * It returns json or xml with respect to acceptance type of the
    *
    * @return the list of the dataset of the user and also public user
    */
  @ApiOperation(value = "Get all datasets",
    notes = "Get the list of the dataset of the user and public user",
    response = classOf[Datasets])
  @ApiImplicitParams(Array(new ApiImplicitParam(name = "X-AUTH-TOKEN", dataType = "string", paramType = "header", required = true)))
  @ApiResponses(value = Array(new ApiResponse(code = 401, message = "User is not authenticated")))
  def getDatasets = AuthenticatedAction { implicit request =>
    val username: String = request.username.get

    lazy val datasets: Datasets = {
      val datasetList = (for (ds: IRDataSet <- repository.listAllDSs(username)) yield Dataset(ds.position, Some(username), info = Some(DatasetUtils.getInfo(username, ds.position)))) ++
        // public dataset
        (for (ds: IRDataSet <- repository.listAllDSs("public")) yield Dataset(ds.position, Some("public"), info = Some(DatasetUtils.getInfo("public", ds.position))))
      Datasets(datasetList.sorted)
    }

    render {
      case Accepts.Xml() => Ok(scala.xml.Utility.trim(datasets.getXml))
      case Accepts.Json() => Ok(Json.toJson(datasets))
      case _ => NA
    }
  }


  /**
    * Return the samples list of the dataset.
    *
    * @param datasetName name of the dataset.
    *                    If starts with <i>"public."</i> then it retrieves from public user dataset.
    * @return
    */
  @ApiOperation(value = "Get all samples of the dataset",
    notes = "Get the list of the samples of the input dataset",
    response = classOf[Dataset])
  @ApiImplicitParams(Array(new ApiImplicitParam(name = "X-AUTH-TOKEN", dataType = "string", paramType = "header", required = true)))
  @ApiResponses(value = Array(
    new ApiResponse(code = 401, message = "User is not authenticated"),
    new ApiResponse(code = 404, message = "Dataset is not found for the user")))
  def getSamples(datasetName: String) = AuthenticatedAction { implicit request =>
    var username: String = request.username.get
    var dsName = datasetName
    // if public then user name is public and get the correct dataset name
    if (datasetName.startsWith("public.")) {
      username = "public"
      dsName = dsName.substring("public.".length)
    }
    try {
      lazy val sampleList = DatasetUtils.getSamples(username, dsName)
      lazy val dataset = Dataset(datasetName, Some(username), samples = Some(sampleList))

      render {
        case Accepts.Xml() => Ok(dataset.getXml)
        case Accepts.Json() => Ok(Json.toJson(dataset))
        case _ => NA
      }
    } catch {
      case _: GMQLDSNotFound => renderedError(NOT_FOUND, "Dataset not found")
    }
  }

  /**
    * Delete the dataset.
    *
    * @param datasetName name of the dataset.
    *                    If starts with <i>"public."</i> then it is about public dataset, which is forbidden to delete.
    * @return
    */
  @ApiOperation(value = "Delete the dataset",
    notes = "Delete the input dataset")
  @ApiImplicitParams(Array(new ApiImplicitParam(name = "X-AUTH-TOKEN", dataType = "string", paramType = "header", required = true)))
  @ApiResponses(value = Array(
    new ApiResponse(code = 401, message = "User is not authenticated"),
    new ApiResponse(code = 403, message = "Public datasets cannot be deleted by user"),
    new ApiResponse(code = 404, message = "Dataset is not found for the user")))
  def deleteDataset(datasetName: String) = AuthenticatedAction { implicit request =>
    val username: String = request.username.get
    if (datasetName.startsWith("public."))
      renderedError(FORBIDDEN, "Public dataset cannot be deleted.")
    else {
      //TODO add also DSExists after its implementation
      try {
        // set as lazy in if the header accept is not correct one
        lazy val result = {
          Logger.debug(s"Deleting dataset of user: $username dataset: $datasetName")
          repository.deleteDS(datasetName, username)
          "Ok"
        }
        render {
          case Accepts.Xml() => Ok(<result>
            {result}
          </result>)
          case Accepts.Json() => Ok(JsObject(Seq("result" -> JsString(result))))
          case _ => NA
        }
      } catch {
        case _: GMQLDSNotFound => renderedError(NOT_FOUND, "Dataset not found")
      }
    }
  }

  /**
    * Rename the dataset.
    *
    * @param datasetName name of the dataset.
    *                    If starts with <i>"public."</i> then it is about public dataset, which is forbidden to rename.
    * @return
    */
  @ApiOperation(value = "Rename the dataset - 2",
    notes = "Rename the input dataset")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "newName", paramType = "query", dataType = "string", required = true),
    new ApiImplicitParam(name = "X-AUTH-TOKEN", dataType = "string", paramType = "header", required = true)))
  @ApiResponses(value = Array(
    new ApiResponse(code = 401, message = "User is not authenticated"),
    new ApiResponse(code = 403, message = "Public datasets cannot be modified by user"),
    new ApiResponse(code = 404, message = "Dataset is not found for the user")))
  def modifyDataset(datasetName: String) = AuthenticatedAction(parse.empty) { implicit request =>
    val newDatasetNameOption = request.getQueryString("newName")
    modifyCommon(request, datasetName, newDatasetNameOption)
  }

  /**
    * Rename the dataset.
    *
    * @param datasetName name of the dataset.
    *                    If starts with <i>"public."</i> then it is about public dataset, which is forbidden to rename.
    * @return
    */
  @ApiOperation(value = "Rename the dataset - 1",
    notes = "Rename the input dataset")
  @ApiImplicitParams(Array(
    //    new ApiImplicitParam(name = "newName", paramType = "query", dataType = "string", required = true),
    new ApiImplicitParam(name = "X-AUTH-TOKEN", dataType = "string", paramType = "header", required = true)))
  @ApiResponses(value = Array(
    new ApiResponse(code = 401, message = "User is not authenticated"),
    new ApiResponse(code = 403, message = "Public datasets cannot be modified by user"),
    new ApiResponse(code = 404, message = "Dataset is not found for the user")))
  def renameDataset(datasetName: String, newDatasetName: String) = AuthenticatedAction(parse.empty) { implicit request =>
    modifyCommon(request, datasetName, Some(newDatasetName))
  }

  def modifyCommon[A](implicit request: AuthenticatedRequest[A], datasetName: String, newDatasetNameOption: Option[String]) = {
    val username: String = request.username.get
    if (datasetName.startsWith("public."))
      renderedError(FORBIDDEN, "Public dataset cannot be modified.")
    else if (newDatasetNameOption.isEmpty)
      renderedError(UNPROCESSABLE_ENTITY, "New name is missing")
    else if (newDatasetNameOption.get == datasetName)
      renderedError(UNPROCESSABLE_ENTITY, "There is nothing to change")
    else {
      val newDatasetName = newDatasetNameOption.get
      //TODO add also DSExists after its implementation
      try {
        // set as lazy in if the header accept is not correct one
        lazy val result = {
          Logger.debug(s"Renaming dataset of user: $username dataset: $datasetName")
          repository.changeDSName(datasetName, username, newDatasetName)
          "Ok"
        }
        render {
          case Accepts.Xml() => Ok(<result>
            {result}
          </result>)
          case Accepts.Json() => Ok(JsObject(Seq("result" -> JsString(result))))
          case _ => NA
        }
      } catch {
        case e: GMQLNotValidDatasetNameException => renderedError(NOT_FOUND, e.getMessage)
        case _: GMQLDSNotFound => renderedError(NOT_FOUND, "Dataset not found")
      }
    }
  }


  /**
    *
    * @param datasetName name of the dataset
    * @param sampleName  name of the sample
    * @param isMeta      is it meta or region file
    * @return the result as text file to the user. The header contains also
    */
  private def getStream(datasetName: String, sampleName: String, isMeta: Boolean) = AuthenticatedAction { implicit request =>
    import scala.util.Try
    var dsName = datasetName
    val topK: Int = request.getQueryString("top").flatMap(s => Try(s.toInt).toOption).getOrElse(Integer.MAX_VALUE)
    val header: Boolean = request.getQueryString("header").flatMap(s => Try(s.toBoolean).toOption).getOrElse(false)
    val bed6: Boolean = request.getQueryString("bed6").flatMap(s => Try(s.toBoolean).toOption).getOrElse(false)

    import scala.concurrent.ExecutionContext.Implicits.global
    val transform = Enumeratee.map[String] { line =>
      val newLine = line + lineSeparator
      newLine.getBytes
    }

    var username: String = request.username.get
    if (dsName.startsWith("public.")) {
      username = "public"
      dsName = dsName.replace("public.", "")
    }

    val bed6Headers = Array("chr", "left", "right", "name", "score", "strand")

    //    else {
    try {
      //TODO use ARM solution, if it is possible
      val (streamRegion, streamMeta) = repository.sampleStreams(dsName, username, sampleName)
      val headerContent: Enumerator[Array[Byte]] =
        if (header) {
          val headerString =
            if (isMeta)
              "Attribute\tValue"
            else {
              if (bed6)
                bed6Headers.mkString("\t")
              else
                repository.getSchema(dsName, username).fields.map(_.name).mkString("\t")
            }
          Enumerator(headerString).through(transform)
        }
        else
          Enumerator.empty

      val stream: InputStream = if (isMeta) {
        streamRegion.close
        streamMeta
      } else {
        streamMeta.close
        streamRegion
      }

      object Foo {
        val MY_STRINGS = Array("chr", "left", "right", "name", "score", "strand")
      }



      def parseAndCorrect(line: String): String = {
        val gmqlSchema: GMQLSchema = repository.getSchema(dsName, username)
        if (bed6 && line != null && line.nonEmpty)
          gmqlSchema.schemaType match {
            case GMQLSchemaFormat.GTF =>
              val splitLine: Array[String] = line.split("\t")
              //0:chr 6:strand
              line + s""" id=${splitLine(0)}${splitLine(6)}; """
            case GMQLSchemaFormat.TAB =>
              val zipped = (gmqlSchema.fields.map(_.name) zip line.split("\t")).toMap
              bed6Headers.map { columnName =>
                var value = zipped.getOrElse(columnName, ".")
                if (columnName == "strand" && (value == "*"))
                  value = "."
                value
              }.mkString("\t")
          }
        else
          line
      }

      val fileContent: Enumerator[Array[Byte]] = {
        import play.api.libs.iteratee._
        lazy val bufferedReader = new BufferedReader(new InputStreamReader(stream))
        var count = topK
        val fileStream: Enumerator[String] = Enumerator.generateM[String] {
          scala.concurrent.Future {
            val line: String =
              if (count > 0) {
                val temp = bufferedReader.readLine()
                if (!isMeta)
                  parseAndCorrect(temp)
                else
                  temp
              } else {
                bufferedReader.close()
                null
              }
            count -= 1
            Option(line)
          }
        }
        fileStream.through(transform)
      }
      Ok.chunked(headerContent >>> fileContent).withHeaders(
        "Content-Type" -> "text/plain",
        "Content-Disposition" -> s"attachment; filename=$dsName-$sampleName${if (isMeta) ".meta" else ""}"
      )
    } catch {
      case _: GMQLDSNotFound => renderedError(NOT_FOUND, s"Dataset not found: $dsName")
      case _: GMQLSampleNotFound => renderedError(NOT_FOUND, s"Sample not found: $dsName-$sampleName")
    }
    //    }
  }

  /**
    *
    * @param datasetName
    * @param sampleName
    * @return
    */
  @ApiOperation(value = "Download sample region",
    notes = "Download region data as stream",
    produces = "file",
    tags = Array("Download repository", SwaggerUtils.swaggerRepository))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "top", paramType = "query", dataType = "integer"),
    new ApiImplicitParam(name = "header", paramType = "query", dataType = "boolean"),
    new ApiImplicitParam(name = "X-AUTH-TOKEN", dataType = "string", paramType = "header", required = true)))
  @ApiResponses(value = Array(
    new ApiResponse(code = 401, message = "User is not authenticated"),
    new ApiResponse(code = 403, message = "Public datasets cannot be downloaded by user"),
    new ApiResponse(code = 404, message = "Dataset or its sample is not found for the user")))
  def getRegionStream(datasetName: String, sampleName: String) = getStream(datasetName, sampleName, isMeta = false)

  @ApiOperation(value = "Download sample metadata",
    notes = "Download metadata data as stream",
    produces = "file",
    tags = Array("Download repository", SwaggerUtils.swaggerRepository))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "top", paramType = "query", dataType = "integer"),
    new ApiImplicitParam(name = "header", paramType = "query", dataType = "boolean"),
    new ApiImplicitParam(name = "X-AUTH-TOKEN", dataType = "string", paramType = "header", required = true)))
  @ApiResponses(value = Array(
    new ApiResponse(code = 401, message = "User is not authenticated"),
    new ApiResponse(code = 403, message = "Public datasets cannot be downloaded by user"),
    new ApiResponse(code = 404, message = "Dataset or its sample is not found for the user")))
  def getMetadataStream(datasetName: String, sampleName: String) = getStream(datasetName, sampleName, isMeta = true)


  @ApiOperation(value = "Download dataset query",
    notes = "Download dataset query if exists as stream",
    produces = "file",
    tags = Array("Download repository", SwaggerUtils.swaggerRepository))
  @ApiImplicitParams(Array(new ApiImplicitParam(name = "X-AUTH-TOKEN", dataType = "string", paramType = "header", required = true)))
  @ApiResponses(value = Array(
    new ApiResponse(code = 401, message = "User is not authenticated"),
    new ApiResponse(code = 403, message = "Public datasets cannot be downloaded by user"),
    new ApiResponse(code = 404, message = "Dataset or its sample is not found for the user")))
  def getQueryStream(datasetName: String) = AuthenticatedAction {
    implicit request =>
      import scala.concurrent.ExecutionContext.Implicits.global
      val username: String = request.username.get
      if (datasetName.startsWith("public."))
        renderedError(FORBIDDEN, "Public dataset cannot be downloaded.")
      else {
        try {
          val scriptStreamOption = try {
            Some(repository.getScriptStream(datasetName, username))
          } catch {
            case _: Throwable => None
          }

          if (scriptStreamOption.isDefined) {
            val fileContent: Enumerator[Array[Byte]] = Enumerator.fromStream(scriptStreamOption.get)
            Ok.chunked(fileContent).withHeaders(
              "Content-Type" -> "text/plain",
              "Content-Disposition" -> s"attachment; filename=$datasetName.gmql"
            )
          }
          else
            renderedError(NOT_FOUND, s"Dataset query not found: $datasetName")
        } catch {
          case _: GMQLDSNotFound => renderedError(NOT_FOUND, s"Dataset not found: $datasetName")
        }
      }
  }


  @ApiOperation(value = "Download dataset vocabulary",
    notes = "Download dataset vocabulary if exists as stream",
    produces = "file",
    tags = Array("Download repository", SwaggerUtils.swaggerRepository))
  @ApiImplicitParams(Array(new ApiImplicitParam(name = "X-AUTH-TOKEN", dataType = "string", paramType = "header", required = true)))
  @ApiResponses(value = Array(
    new ApiResponse(code = 401, message = "User is not authenticated"),
    new ApiResponse(code = 403, message = "Public datasets cannot be downloaded by user"),
    new ApiResponse(code = 404, message = "Dataset or its sample is not found for the user")))
  def getVocabularyStream(datasetName: String) = AuthenticatedAction {
    implicit request =>
      val username: String = request.username.get
      if (datasetName.startsWith("public."))
        renderedError(FORBIDDEN, "Public dataset cannot be downloaded.")
      else {
        import scala.concurrent.ExecutionContext.Implicits.global
        try {
          val scriptStreamOption = try {
            Some(repository.getVocabularyStream(datasetName, username))
          } catch {
            case _: Throwable => None
          }

          if (scriptStreamOption.isDefined) {
            val fileContent: Enumerator[Array[Byte]] = Enumerator.fromStream(scriptStreamOption.get)
            Ok.chunked(fileContent).withHeaders(
              "Content-Type" -> "text/plain",
              "Content-Disposition" -> s"attachment; filename=$datasetName.vocabulary"
            )
          }
          else
            renderedError(NOT_FOUND, s"Dataset query not found: $datasetName")
        } catch {
          case _: GMQLDSNotFound => renderedError(NOT_FOUND, s"Dataset not found: $datasetName")
        }
        //        Ok("N/A yet").withHeaders(
        //          "Content-Type" -> "text/plain",
        //          "Content-Disposition" -> s"attachment; filename=$datasetName.gmql"
        //        )
      }
  }


  /**
    * returns the sample
    *
    * @param datasetName
    * @return
    */
  @ApiOperation(value = "Download dataset as zip file",
    notes = "Download dataset as zip stream",
    produces = "file",
    tags = Array("Download repository", SwaggerUtils.swaggerRepository))
  @ApiImplicitParams(Array(new ApiImplicitParam(name = "X-AUTH-TOKEN", dataType = "string", paramType = "header", required = true)))
  @ApiResponses(value = Array(
    new ApiResponse(code = 401, message = "User is not authenticated"),
    new ApiResponse(code = 403, message = "Public datasets cannot be downloaded by user"),
    new ApiResponse(code = 404, message = "Dataset is not found for the user")))
  def zip(datasetName: String) = AuthenticatedAction { implicit request =>

    import play.api.libs.concurrent.Execution.Implicits.defaultContext
    val username: String = request.username.get

    try {
      if (datasetName.startsWith("public."))
        renderedError(FORBIDDEN, "Public dataset cannot be downloaded.")
      else {
        val sampleNames = repository.listDSSamples(datasetName, username).map(temp => (temp.name.split("/").last.split("\\.").head, temp.name.split("/").last))
        Logger.debug("sampleNames" + sampleNames)

        val vocabularyCount = new VocabularyCount
        //TODO add schema

        val sources = sampleNames.flatMap { sampleName =>
          lazy val streams = repository.sampleStreams(datasetName, username, sampleName._1)
          List(
            ZipEnumerator.Source(s"$datasetName/files/${sampleName._2}", { () => Future(Some(streams._1)) }),
            ZipEnumerator.Source(s"$datasetName/files/${sampleName._2}.meta", { () => Future(Some(vocabularyCount.addVocabulary(streams._2))) })
          )
        }
        lazy val schemaStream = repository.getSchemaStream(datasetName, username)
        sources += ZipEnumerator.Source(s"$datasetName/files/${datasetName}.schema", { () => Future(Some(schemaStream)) })


        val scriptStreamTest = try {
          Some(repository.getScriptStream(datasetName, username).close())
        } catch {
          case _: Throwable => None
        }

        if (scriptStreamTest.isDefined) {
          lazy val scriptStream = repository.getScriptStream(datasetName, username)
          sources += ZipEnumerator.Source(s"$datasetName/$datasetName.gmql", { () => Future(Some(scriptStream)) })
        }

        sources += ZipEnumerator.Source(s"$datasetName/vocabulary.txt", { () => Future(Some(vocabularyCount.getStream)) })

        Logger.debug(s"Before zip enumerator: $username->$datasetName")
        Ok.chunked(ZipEnumerator(sources))(play.api.http.Writeable.wBytes).withHeaders(
          CONTENT_TYPE -> "application/zip",
          CONTENT_DISPOSITION -> s"attachment; filename=$datasetName.zip"
        )
      }
    } catch {
      case _: GMQLDSNotFound => renderedError(NOT_FOUND, s"Dataset not found: $datasetName")
    }
  }

  @ApiOperation(value = "Get shema of the dataset",
    notes = "Get the schema field of the input dataset",
    response = classOf[GMQLSchema])
  @ApiImplicitParams(Array(new ApiImplicitParam(name = "X-AUTH-TOKEN", dataType = "string", paramType = "header", required = true)))
  @ApiResponses(value = Array(
    new ApiResponse(code = 401, message = "User is not authenticated"),
    new ApiResponse(code = 404, message = "Dataset is not found for the user")))
  def dataSetSchema(datasetName: String) = AuthenticatedAction { implicit request =>
    var username = request.username.getOrElse("")
    var dsName = datasetName
    if (datasetName.startsWith("public.")) {
      username = "public"
      dsName = dsName.substring("public.".length)
    }

    def getXmlGmqlSchema(gmqlSchema: GMQLSchema): Elem =
        <schema>
          <name>{gmqlSchema.name}</name>
          <type>{gmqlSchema.schemaType}</type>
          {gmqlSchema.fields.map(getXmlGmqlSchemaField)}
        </schema>

    def getXmlGmqlSchemaField(gmqlSchemaField: GMQLSchemaField): Elem =
      <field>
        <name>{gmqlSchemaField.name}</name>
        <type>{gmqlSchemaField.fieldType}</type>
      </field>

    implicit val writerGmqlSchemaField = (
      (JsPath \ "name").write[String] and
        (JsPath \ "type").write[ParsingType.Value]
      ) (unlift(GMQLSchemaField.unapply))

    implicit val writerGmqlSchema = (
      (JsPath \ "name").write[String] and
        (JsPath \ "type").write[GMQLSchemaFormat.Value] and
        (JsPath \ "coordinate_system").write[GMQLSchemaCoordinateSystem.Value] and
        (JsPath \ "fields").write[List[GMQLSchemaField]]
      ) (unlift(GMQLSchema.unapply))

    try {
      lazy val gmqlSchema = repository.getSchema(dsName, username)
      //      lazy val gmqlSchema: GMQLSchema = GMQLSchema("Test", GMQLSchemaTypes.Delimited, List(GMQLSchemaField("COL1", ParsingType.DOUBLE), GMQLSchemaField("COL2", ParsingType.INTEGER)))

      render {
        case Accepts.Xml() => Ok(getXmlGmqlSchema(gmqlSchema))
        case Accepts.Json() => Ok(Json.toJson(gmqlSchema))
        case _ => NA
      }
    } catch {
      case _: GMQLDSNotFound => renderedError(NOT_FOUND, "Dataset not found")
    }
  }


  @ApiOperation(value = "getUcscLink", hidden = true)
  @ApiImplicitParams(Array(new ApiImplicitParam(name = "X-AUTH-TOKEN", dataType = "string", paramType = "header", required = true)))
  def getUcscLink(datasetName: String) = AuthenticatedAction { implicit request =>
    val username = request.username.get
    //TODO create temp token
    val token = request.authentication.get.authToken
    //      val directory = prepareFile("2", username, dataSetName)
    if (datasetName.startsWith("public."))
      BadRequest("Cannot load public datasets")
    else
      Ok(s"${controllers.gmql.routes.DSManager.getUcscList(datasetName).absoluteURL()}?authToken=$token")
  }

  @ApiOperation(value = "getUcscList", hidden = true)
  @ApiImplicitParams(Array(new ApiImplicitParam(name = "X-AUTH-TOKEN", dataType = "string", paramType = "header", required = true)))
  def getUcscList(datasetName: String) = AuthenticatedAction { implicit request =>
    // http://genome.ucsc.edu/cgi-bin/hgTracks?org=human&hgt.customText=http://genomic.elet.polimi.it/gmql-rest/dataSet/heatmap/parse2?auth-token=test-best-token
    // http://genome.ucsc.edu/cgi-bin/hgTracks?org=human&hgt.customText=http://www.bioinformatics.deib.polimi.it/canakoglu/test2.txt
    // val username = request.username.getOrElse("")
    val username = request.username.get
    //    val directory = prepareFile("2", username, dataSetName)
    //    val fileList = new File(directory).listFiles.filter(file => !file.getName.endsWith(".gmql") && !file.getName.endsWith(".meta") && !file.getName.endsWith(".crc") && !file.getName.endsWith(".schema")).map(_.getName)

    //TODO create temp token

    val token = request.authentication.get.authToken


    if (datasetName.startsWith("public."))
      BadRequest("Cannot load public datasets")
    else {
      val buf = new StringBuilder
      //    buf ++= "browser position chr1:270-1100"
      lazy val sampleList = DatasetUtils.getSamples(username, datasetName)

      for (sample <- sampleList) {
        //      Logger.debug("file asd:" + file)
        val trackName: String = datasetName + "-" + sample.name
        val description = trackName
        buf ++= s"""track name=\"$trackName\" description=\"$description\"  useScore=1 visibility=\"3\" $lineSeparator"""
        //      buf ++= s"""track name=\"$trackName\" $newLine"""
        buf ++= s"${controllers.gmql.routes.DSManager.getRegionStream(datasetName, sample.name).absoluteURL()}?authToken=$token&bed6=true $lineSeparator"
      }
      Ok(buf.toString)
    }
  }

  private def getMetadata2(fileName: String) = {
    val columnNames = Map(
      "S_-8820234713312881879" -> "Leukaemia",
      "S_-8750901162148023240" -> "Breast cancer",
      "S_-5793246318738746351" -> "Prostate cancer",
      "S_-6670097956223382349" -> "Melanoma",
      "S_-7703343600216302069" -> "Lung cancer",
      "S_-1951876333163453708" -> "Cervical cancer",
      "S_-2154111864833666186" -> "Sarcoma",
      "S_6934711042323284719" -> "Brain tumour",
      "S_98502003476172547" -> "Thyroid cancer",
      "S_-825433199616962772" -> "Lymphoma",
      "S_5644747271103887797" -> "Thymus cancer",
      "S_-1862735225906400534" -> "Pancreatic cancer",


      "S_-3535690899313408313" -> "Leukaemia",
      "S_-5848978898296609239" -> "Breast cancer",
      "S_-6329371575916788697" -> "Prostate cancer",
      "S_-679136028966635093" -> "Melanoma",
      "S_160316112154375081" -> "Lung cancer",
      "S_2428267966423886576" -> "Cervical cancer",
      "S_3516814249732603062" -> "Sarcoma",
      "S_7344915156143663700" -> "Brain tumour",
      "S_790155058247958618" -> "Thyroid cancer",
      "S_8111978318431447731" -> "Lymphoma",
      "S_8915483448991498418" -> "Thymus cancer",
      "S_8993735038566083453" -> "Pancreatic cancer"
    )
    columnNames.get(fileName).getOrElse(fileName)
  }

  @ApiOperation(value = "parseFiles", hidden = true)
  @ApiImplicitParams(Array(new ApiImplicitParam(name = "X-AUTH-TOKEN", dataType = "string", paramType = "header", required = true)))
  def parseFiles(datasetName: String, columnName: String) = AuthenticatedAction { implicit request =>
    import org.apache.hadoop.fs.{FileSystem, Path}
    val username = request.username.getOrElse("")
    val directory = ""
    //prepareFile("2", username, dataSetName)
    val isCount = columnName.startsWith("count_")

    val conf = FS_Utilities.gethdfsConfiguration()
    val fs = FileSystem.get(conf)
    val hdfspath = conf.get("fs.defaultFS") + ut.getHDFSRegionDir(username)

    val samples: Seq[Sample] = DatasetUtils.getSamples(username, datasetName)
    val samplePath = samples.head.path.get


    ut.HADOOP_HOME + ut.getHDFSRegionDir(username)

    val parser = new CustomParser()

    val schemaPath = new Path(hdfspath + samplePath).getParent.toString
    parser.setSchema(schemaPath)
    var count = -1

    def f(x: String) = {
      count += 1
      count
    }

    val cache = scala.collection.mutable.LinkedHashMap.empty[String, Int]

    def cachedF(s: String) = cache.getOrElseUpdate(s, f(s))


    val optionColumn = parser.schema.map(_._1).indexOf(columnName)
    if (optionColumn == -1)
      throw new Exception("Unknown column")

    //foreach file

    Logger.debug("before file list")
    //    val fileList = new File(directory).listFiles.filter(file => !file.getName.endsWith(".gmql") && !file.getName.endsWith(".meta") && !file.getName.endsWith(".crc") && !file.getName.endsWith(".schema")).map(_.getCanonicalPath)

    Logger.debug("after file list")
    val wholeListBuffer = samples.map { sample: Sample =>
      Logger.debug("Sample:" + sample)
      val tempList: ListBuffer[GValue] = ListBuffer[GValue]()
      for (tuple <- parseHelper(username, datasetName, parser, sample, optionColumn, 20)) {
        val colId = cachedF(tuple._1)
        while (colId > tempList.length)
          tempList += {
            if (isCount) new GInt(0) else new GNull()
          }
        if (colId == tempList.length)
          tempList += tuple._2
        else
          tempList.update(colId, tuple._2)
      }
      tempList
    }

    var rowNames = cache.keys.toList
    var rowLength = rowNames.length
    //    if (rowLength > 10)
    //      rowLength = 10
    rowNames = rowNames.slice(0, rowLength)

    var columnNames = (for (sample <- samples) yield sample.name).toList
    val columnLength = columnNames.length


    //I can skip this TODO in the next step
    val wholeList = wholeListBuffer.map { tempList =>
      while (rowLength > tempList.length)
        tempList += {
          if (isCount) new GInt(0) else new GNull()
        }
      tempList.toList
    }
    val result = for (i <- 0 to rowLength - 1; j <- 0 to columnLength - 1)
      yield wholeList(j)(i).toString

    //        println(result)
    //    columnNames = List("Leukaemia",
    //      "Breast cancer",
    //      "Prostate cancer",
    //      "Melanoma",
    //      "Lung cancer",
    //      "Cervical cancer",
    //      "Sarcoma",
    //      "Brain tumour",
    //      "Thyroid cancer",
    //      "Lymphoma",
    //      "Thymus cancer",
    //      "Pancreatic cancer")
    Ok(views.html.heat_map(result.toList, rowLength, columnLength, rowNames, columnNames))
  }

  private def parseHelper(username: String, datasetName: String, parser: CustomParser, sample: Sample, column: Int, maxRow: Int): Iterator[(String, GValue)] = {
    Logger.info(sample.toString)


    var returnVal = List.empty[(String, GValue)].iterator

    import resource._

    import scala.io.Source


    //    http://jsuereth.com/scala-arm/usage.html
    for {
      file <- managed(repository.sampleStreams(datasetName, username, sample.name) match { case (region: InputStream, meta: InputStream) => meta.close(); region })
    } {
      val fileLines = Source.fromInputStream(file).getLines()

      val res2 = new ListBuffer[(String, GValue)]

      var count = 0
      while (fileLines.hasNext && count < maxRow) {
        val line = fileLines.next()
        val parsed: Option[(GRecordKey, Array[GValue])] = parser.region_parser((999, line))

        val temp = if (parsed.isDefined) {
          val getParsed = parsed.get
          val getParsed1 = getParsed._1
          val getParsed2: Array[GValue] = getParsed._2
          //        Logger.debug("getParsed1" + getParsed1)
          (s"${
            getParsed1._2
          }_${
            getParsed1._3
          }_${
            getParsed1._4
          }", getParsed2(column))
        }
        else
          ("unknown", new GNull)

        res2 += temp
        count += 1
      }

      returnVal = res2.toIterator
    }

    returnVal
    // OLD IMPLEMENTATION
    //    val res: Iterator[(String, GValue)] = Source.fromFile(fileName).getLines.map { line =>
    ////      println(line)
    //      val parsed: Option[(GRecordKey, Array[GValue])] = parser.region_parser((999, line))
    ////      println(parsed)
    ////      val list: Array[String] = parsed.get._2.map(x => x.toString)
    ////      println(parsed.get._2.mkString(", "))
    //      if (parsed.isDefined) {
    //        val getParsed = parsed.get
    //        val getParsed1 = getParsed._1
    //        val getParsed2: Array[GValue] = getParsed._2
    ////        Logger.debug("getParsed1" + getParsed1)
    //        (s"${getParsed1._2}_${getParsed1._3}_${getParsed1._4}_${getParsed1._5}", getParsed2(column))
    //      }
    //      else
    //        ("unknown", new GNull)
    //    }
    //    //    println("qwe:" + res.toList)
    //    res
  }

  @ApiOperation(value = "Upload dataset",
    notes = "Upload dataset with samples. In this example interface, user can upload up to 4 files",
    consumes = "application/x-www-form-urlencoded",
    response = classOf[UploadResult])
  @ApiResponses(value = Array(
    //    new ApiResponse(code = 200, message = "test"),
    new ApiResponse(code = 401, message = "User is not authenticated"),
    new ApiResponse(code = 404, message = "Dataset is not found for the user")
  ))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "schemaName", paramType = "query", dataType = "string", allowableValues = "bed, bedGraph, NarrowPeak, BroadPeak, vcf"),
    new ApiImplicitParam(name = "schema", dataType = "file", paramType = "form"),
    new ApiImplicitParam(name = "file1", dataType = "file", paramType = "form"),
    new ApiImplicitParam(name = "file2", dataType = "file", paramType = "form"),
    new ApiImplicitParam(name = "file3", dataType = "file", paramType = "form"),
    new ApiImplicitParam(name = "file4", dataType = "file", paramType = "form"),
    new ApiImplicitParam(name = "X-AUTH-TOKEN", dataType = "string", paramType = "header", required = true)))
  def uploadSample(dataSetName: String) = AuthenticatedAction(parse.multipartFormData) { implicit request =>
    val schemaNameOption = request.getQueryString("schemaName")

    val username = request.username.get
    Logger.debug("uploadSample => username: " + username)
    //TODO move create empty directory to utils
    val tempDirPath = DatasetUtils.createEmptyTempDirectory(username, dataSetName)

    var isSchemaUploaded = false
    Logger.debug("uploadSample=>tempDirPath: " + tempDirPath)
    try {
      val files = mutable.Set.empty[String]
      request.body.files.foreach {
        file =>
          val newFile =
            if (file.key == "schema") {
              isSchemaUploaded = true
              new File(tempDirPath + ".schema")
            } else {
              files += tempDirPath + File.separator + file.filename
              new File(tempDirPath + File.separator + file.filename)
            }
          file.ref.moveTo(newFile)
          Logger.info("File: " + file.filename)
      }
      Logger.info("Schema name: " + schemaNameOption.getOrElse("NO INPUT"))
      val schemaPathOption = getSchemaPath(tempDirPath, isSchemaUploaded, schemaNameOption)


      importDataset(username, dataSetName, schemaPathOption, tempDirPath, files.toSet)
    }
    catch {
      case e: GMQLNotValidDatasetNameException =>
        Logger.error("error", e)
        val message = " \n" + e.getMessage
        BadRequest(message)
      case e: SAXException =>
        Logger.error("error", e)
        val message = " The dataset schema does not confirm the schema style (XSD) \n" + e.getMessage
        BadRequest(message)
      case e: Exception =>
        Logger.error("Upload Error", e)
        val message =
          "Unknown error" + {
            if (Play.isDev) ": " + e.getMessage else ""
          }
        BadRequest(message)
    } finally {
      DatasetUtils.deleteTemp(tempDirPath)
    }
  }


  @ApiOperation(value = "Upload dataset from URL",
    notes = "Upload dataset from another server.",
    consumes = "application/json",
    response = classOf[UploadResult]
  )
  @ApiResponses(value = Array(
    //    new ApiResponse(code = 200, message = "test"),
    new ApiResponse(code = 401, message = "User is not authenticated"),
    new ApiResponse(code = 404, message = "Dataset is not found for the user")
  ))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "schemaName", paramType = "query", dataType = "string", allowableValues = "[bed, bedGraph, NarrowPeak, BroadPeak, vcf]"),
    new ApiImplicitParam(name = "body", dataType = "string", paramType = "body",
      examples = new Example(Array(new ExampleProperty(value = "{\n\t\"schema_file\": \"http://www.bioinformatics.deib.polimi.it/canakoglu/guest_data/HG19_ANN.schema\",\n\t\"data_files\": [\n\t\t\"http://www.bioinformatics.deib.polimi.it/canakoglu/guest_data/RefSeqGenesExons_hg19.bed\",\n\t\t\"http://www.bioinformatics.deib.polimi.it/canakoglu/guest_data/RefSeqGenesExons_hg19.bed.meta\",\n\t\t\"http://www.bioinformatics.deib.polimi.it/canakoglu/guest_data/TSS_hg19.bed\",\n\t\t\"http://www.bioinformatics.deib.polimi.it/canakoglu/guest_data/TSS_hg19.bed.meta\",\n\t\t\"http://www.bioinformatics.deib.polimi.it/canakoglu/guest_data/VistaEnhancers_hg19.bed\",\n\t\t\"http://www.bioinformatics.deib.polimi.it/canakoglu/guest_data/VistaEnhancers_hg19.bed.meta\"\n\t]\n}")))
    ), new ApiImplicitParam(name = "X-AUTH-TOKEN", dataType = "string", paramType = "header", required = true)))
  def uploadSamplesFromUrls(dataSetName: String) = AuthenticatedAction(parse.json) {
    implicit request =>
      val schemaNameOption = request.getQueryString("schemaName")

      val username = request.username.get
      val dataFilePaths = (request.body \ "data_files").asOpt[Seq[String]].getOrElse(Seq.empty[String])
      val schemaFileOpt = (request.body \ "schema_file").asOpt[String]

      val tempDirPath = DatasetUtils.createEmptyTempDirectory(username, dataSetName)
      try {
        val files = mutable.Set.empty[String]
        dataFilePaths.foreach {
          filePath =>
            files += downloadFile(tempDirPath, filePath)
        }
        val schemaPath = schemaFileOpt.map(schemaFilePath =>
          downloadFile(new File(tempDirPath + ".schema"), schemaFilePath)
        )

        Logger.info("Schema name: " + schemaNameOption.getOrElse("NO INPUT"))

        val schemaPathOption = getSchemaPath(tempDirPath, schemaPath.isDefined, schemaNameOption)


        importDataset(username, dataSetName, schemaPathOption, tempDirPath, files.toSet)

      } catch {
        case e: FileNotFoundException =>
          Logger.error("error", e)
          val message = "File not found: " + e.getMessage + ". Please check the URLs and call the service again"
          BadRequest(message)
        case e: SAXException =>
          Logger.error("error", e)
          val message = " The dataset schema does not confirm the schema style (XSD) \n" + e.getMessage
          BadRequest(message)
        case e: Exception =>
          Logger.error("Upload Error", e)
          val message =
            "Unknown error" + {
              if (Play.isDev) ": " + e.getMessage else ""
            }
          BadRequest(message)
      } finally {
        DatasetUtils.deleteTemp(tempDirPath)
      }

  }

  private def getSchemaPath(tempDirPath: String, isSchemaUploaded: Boolean, schemaNameOption: Option[String]): Option[String] = {
    schemaNameOption match {
      case Some(schemaName) => Some(s"${ut.GMQL_CONF_DIR}/${schemaName.toUpperCase}.schema")
      case None => if (isSchemaUploaded) Some(tempDirPath + ".schema") else None
    }
  }


  private def importDataset(username: String, dataSetName: String, schemaPathOption: Option[String], tempDirPath: String, files: Set[String])(implicit request: RequestHeader) = {
    val samplesTuple3 = createEmptyMeta(tempDirPath, files)
    val samplesToImport = samplesTuple3._1 ++ samplesTuple3._2

    schemaPathOption match {
      case Some(schemaPath) =>
        //      val dataset = IRDataSet(dataSetName, repository.readSchemaFile(schemaPath).fields.map(field => (field.name, field.fieldType)))
        val samples: util.List[GMQLSample] = samplesToImport.toList.map(fileName => GMQLSample(fileName, fileName + ".meta"))
        repository.importDs(dataSetName, username, samples, schemaPath)

        def stringToSample(set: Set[String]) = if (set.isEmpty) None else Some(set.toSeq.map((file: String) => Sample("", file.split("/").last)))

        val uploadResult = UploadResult(stringToSample(samplesTuple3._1).getOrElse(Seq.empty), stringToSample(samplesTuple3._2), stringToSample(samplesTuple3._3))
        render {
          case Accepts.Xml() => Ok(scala.xml.Utility.trim(uploadResult.getXml))
          case Accepts.Json() => Ok(Json.toJson(uploadResult))
          case _ => NA
        }
      //        Ok("Done")
      case None => NotAcceptable("Schema is not defined correctly")
    }
  }

  private def createEmptyMeta(tempDirPath: String, sampleSet: Set[String]) = {
    val (metasTemp, regions) = sampleSet.partition(_.endsWith(".meta"))
    val metas = metasTemp.map(t => t.substring(0, t.size - 5))
    val regionOnly = regions diff metas
    val metaOnly = metas diff regions
    val both = metas intersect regions

    Logger.debug("both      : " + both)
    Logger.debug("regionOnly: " + regionOnly)
    Logger.debug("metaOnly  : " + metaOnly)

    for (regionFile <- regionOnly)
      new PrintWriter(s"$regionFile.meta") {
        write(s"filename\t${regionFile.split("/").last}");
        close
      }

    Logger.debug((both, regionOnly, metaOnly).toString())
    (both, regionOnly, metaOnly)
  }


  //TODO move to utils
  private def downloadFile(tempDirPath: String, filePath: String): String = {
    val urlObj = new URL(filePath)

    val fileName = urlObj.getPath.split("/").last
    val newFile = new File(tempDirPath + File.separator + fileName)
    downloadFile(newFile, filePath)
  }

  //TODO move to utils
  private def downloadFile(newFile: File, filePath: String): String = {
    val urlObj = new URL(filePath)

    //This for getting correct try catch
    urlObj.openStream().close()
    //copy file from url to new file
    val result = urlObj #> newFile !!

    //This will be empty
    Logger.debug("result1:" + result)
    newFile.getAbsolutePath
  }


  @ApiOperation(value = "Get dataset info",
    notes = "Get dataset info",
    response = classOf[Info])
  @ApiImplicitParams(Array(new ApiImplicitParam(name = "X-AUTH-TOKEN", dataType = "string", paramType = "header", required = true)))
  @ApiResponses(value = Array(
    new ApiResponse(code = 401, message = "User is not authenticated"),
    new ApiResponse(code = 404, message = "Dataset is not found for the user")))
  def getDatasetInfo(datasetName: String) = AuthenticatedAction { implicit request =>
    var username: String = request.username.get
    var dsName = datasetName
    // if public then user name is public and get the correct dataset name
    if (datasetName.startsWith("public.")) {
      username = "public"
      dsName = dsName.substring("public.".length)
    }


    try {
      lazy val datasetsInfo = DatasetUtils.getInfo(username, dsName)


      render {
        case Accepts.Xml() => Ok(scala.xml.Utility.trim(datasetsInfo.getXml))
        case Accepts.Json() => Ok(Json.toJson(datasetsInfo))
        case _ => NA
      }
    } catch {
      case _: GMQLDSNotFound => renderedError(NOT_FOUND, "Dataset not found")
    }
  }

  @ApiOperation(value = "Get sample info",
    notes = "Get sample info",
    response = classOf[Info])
  @ApiImplicitParams(Array(new ApiImplicitParam(name = "X-AUTH-TOKEN", dataType = "string", paramType = "header", required = true)))
  @ApiResponses(value = Array(
    new ApiResponse(code = 401, message = "User is not authenticated"),
    new ApiResponse(code = 404, message = "Dataset or its is not found for the user")))
  def getSampleInfo(datasetName: String, sampleName: String) = AuthenticatedAction { implicit request =>
    var dsName = datasetName
    var username: String = request.username.get
    if (dsName.startsWith("public.")) {
      username = "public"
      dsName = dsName.replace("public.", "")
    }

    try {
      lazy val sampleInfo = DatasetUtils.getInfo(username, dsName, sampleName)

      render {
        case Accepts.Xml() => Ok(scala.xml.Utility.trim(sampleInfo.getXml))
        case Accepts.Json() => Ok(Json.toJson(sampleInfo))
        case _ => NA
      }
    } catch {
      case _: GMQLDSNotFound => renderedError(NOT_FOUND, "Dataset not found")
      case _: GMQLSampleNotFound => renderedError(NOT_FOUND, "Sample not found")
    }
  }


  @ApiOperation(value = "Get memory usage",
    notes = "Get memory usage",
    response = classOf[Info])
  @ApiImplicitParams(Array(new ApiImplicitParam(name = "X-AUTH-TOKEN", dataType = "string", paramType = "header", required = true)))
  @ApiResponses(value = Array(
    new ApiResponse(code = 401, message = "User is not authenticated"),
    new ApiResponse(code = 404, message = "Dataset or its is not found for the user")))
  def getMemoryUsage() = AuthenticatedAction { implicit request =>
    val username: String = request.username.get
    val userClass = request.user.get.userType


    try {
      lazy val result = {
        val (occupied, available) = repository.getUserQuotaInfo(username, userClass)
        val isUserQuotaExceeded = repository.isUserQuotaExceeded(username, userClass)
        val map = mutable.Map.empty[String, AnyVal]
        map.put("occupied", occupied)
        map.put("available", available)
        map.put("total", occupied + available)
        map.put("quota_exceeded", isUserQuotaExceeded)
        map.put("used_percentage", (occupied / (occupied + available) * 10000).toInt / 100.0)

        Info(map.toList.map(a => (a._1, a._2.toString)).sorted)
      }


      render {
        case Accepts.Xml() => Ok(scala.xml.Utility.trim(result.getXml))
        case Accepts.Json() => Ok(Json.toJson(result))
        case _ => NA
      }
    } catch {
      case _: GMQLDSNotFound => renderedError(NOT_FOUND, "Dataset not found")
      case _: GMQLSampleNotFound => renderedError(NOT_FOUND, "Sample not found")
    }
  }

}

