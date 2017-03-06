package controllers.gmql

import java.io._
import java.net.URL
import java.util
import javax.inject.Singleton

import controllers.gmql.ResultUtils._
import it.polimi.genomics.core.DataStructures.IRDataSet
import it.polimi.genomics.core.{GNull, _}
import it.polimi.genomics.repository.FSRepository.FS_Utilities
import it.polimi.genomics.repository.GMQLExceptions.{GMQLDSNotFound, GMQLSampleNotFound}
import it.polimi.genomics.repository.{Utilities, _}
import it.polimi.genomics.spark.implementation.loaders.CustomParser
import org.xml.sax.SAXException
import play.api.Play.current
import play.api.libs.iteratee.Enumerator
import play.api.libs.json._
import play.api.mvc._
import play.api.{Logger, Play}
import utils.{GmqlGlobal, VocabularyCount, ZipEnumerator}
import wrappers.authanticate.AuthenticatedAction

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.io.Source
import scala.sys.process._
import scala.xml.Elem


/**
  * Created by Canakoglu on 15-Mar-16.
  */
@Singleton
class DSManager extends Controller {
  val repository: GMQLRepository = GmqlGlobal.repository
  val ut = GmqlGlobal.ut
  val newLine = sys.props("line.separator")


  /**
    * Return the merged list of the dataset of the user and <i>public</i> user.
    * It returns json or xml with respect to acceptance type of the
    *
    * @return the list of the dataset of the user and also public user
    */
  def getDatasets = AuthenticatedAction { implicit request =>
    val username: String = request.username.get

    lazy val datasets = {
      val datasetList = (for (ds: IRDataSet <- repository.listAllDSs(username)) yield Dataset(ds.position, Some(username))) ++
        // public dataset
        (for (ds: IRDataSet <- repository.listAllDSs("public")) yield Dataset(ds.position, Some("public")))
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
  def deleteDataset(datasetName: String) = AuthenticatedAction { implicit request =>
    val username: String = request.username.get
    if (datasetName.startsWith("public."))
      renderedError(UNAUTHORIZED, "Public dataset cannot be deleted")
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
    *
    * @param datasetName
    * @param sampleName
    * @param isMeta
    * @return
    */
  private def getStream(datasetName: String, sampleName: String, isMeta: Boolean) = AuthenticatedAction { implicit request =>
    import scala.concurrent.ExecutionContext.Implicits.global
    val username: String = request.username.get
    try {
      //TODO use ARM solution, if it is possible
      val (streamRegion, streamMeta) = repository.sampleStreams(datasetName, username, sampleName)
      val stream = if (isMeta) {
        streamRegion.close
        streamMeta
      } else {
        streamMeta.close
        streamRegion
      }
      val fileContent: Enumerator[Array[Byte]] = Enumerator.fromStream(stream)
      Ok.chunked(fileContent).withHeaders(
        "Content-Type" -> "text/plain",
        "Content-Disposition" -> s"attachment; filename=$datasetName-$sampleName"
      )
    } catch {
      case _: GMQLDSNotFound => renderedError(NOT_FOUND, s"Dataset not found: $datasetName")
      case _: GMQLSampleNotFound => renderedError(NOT_FOUND, s"Sample not found: $datasetName->$sampleName")
    }
  }

  /**
    *
    * @param datasetName
    * @param sampleName
    * @return
    */
  def getRegionStream(datasetName: String, sampleName: String) = getStream(datasetName, sampleName, isMeta = false)

  def getMetadataStream(datasetName: String, sampleName: String) = getStream(datasetName, sampleName, isMeta = true)

  /**
    * returns the sample
    *
    * @param datasetName
    * @return
    */
  def zip(datasetName: String) = AuthenticatedAction { implicit request =>
    import play.api.libs.concurrent.Execution.Implicits.defaultContext
    val username: String = request.username.get
    val sampleNames = repository.listDSSamples(datasetName, username).map(temp => (temp.name.split("/").last.split("\\.").head, temp.name.split("/").last))
    Logger.debug("sampleNames" + sampleNames)

    val vocabularyCount = new VocabularyCount
    //TODO add schema

    val sources = sampleNames.flatMap { sampleName =>
      lazy val streams = repository.sampleStreams(datasetName, username, sampleName._1)
      List(
        ZipEnumerator.Source(datasetName + "/" + "files" + "/" + sampleName._2, { () => Future(Some(streams._1)) }),
        ZipEnumerator.Source(datasetName + "/" + "files" + "/" + sampleName._2 + ".meta", { () => Future(Some(vocabularyCount.addVocabulary(streams._2))) })
      )
    }

    sources += ZipEnumerator.Source(datasetName + "/" + "vocabulary.txt", { () => Future(Some(vocabularyCount.getStream)) })

    Logger.debug(s"Before zip enumerator: $username->$datasetName")
    Ok.chunked(ZipEnumerator(sources))(play.api.http.Writeable.wBytes).withHeaders(
      CONTENT_TYPE -> "application/zip",
      CONTENT_DISPOSITION -> s"attachment; filename=$datasetName.zip"
    )
  }


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
          <schema_type>{gmqlSchema.schemaType}</schema_type>
          {gmqlSchema.fields.map(getXmlGmqlSchemaField)}
        </schema>

    def getXmlGmqlSchemaField(gmqlSchemaField: GMQLSchemaField): Elem =
      <field>
        <name>{gmqlSchemaField.name}</name>
        <field_type>{gmqlSchemaField.fieldType}</field_type>
      </field>

    implicit val writerGmqlSchemaField = Json.writes[GMQLSchemaField]
    implicit val writerGmqlSchema = Json.writes[GMQLSchema]

    try {
      val gmqlSchema = repository.getSchema(dsName, username)
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


  //  def zipFilePreparation(dataSetName: String, clean: String) = AuthenticatedAction {
  //    request =>
  //      val username = request.username.getOrElse("")
  //      val response = new DataSetsManager().prepareFileZip(username, dataSetName, clean)
  //      resultHelper(response)
  //  }


  //  def downloadFileZip(dataSetName: String) = AuthenticatedAction {
  //    request =>
  //      val username = request.username.getOrElse("")
  //      new DataSetsManager().prepareFileZip(username, dataSetName, "false")
  //      val response = new DataSetsManager().downloadFileZip(username, dataSetName)
  //      val res = resultHelper(response)
  //      res
  //  }

  //  def downloadFileZip2(dataSetName: String) = AuthenticatedAction {
  //    request =>
  //      val username = request.username.getOrElse("")
  //      //    val directory = DataSetsManager.prepareFile("",username, dataSetName)
  //
  //      //    val resultInputStream = new PipedInputStream()
  //      //    val zos = new ZipOutputStream(new PipedOutputStream(resultInputStream))
  //
  //      val dsList: util.List[GMQLSample] = DataSetsManager.repository.ListDSSamples(dataSetName, username)
  //
  //
  //      //    val pos2 = new PipedOutputStream()
  //      //    val pis2 = new PipedInputStream(pos2)
  //
  //      val resultStream = new PipedInputStream()
  //      val outStream = new PrintWriter(new PipedOutputStream(resultStream))
  //
  //      Future {
  //        dsList.asScala.foreach(sample => outStream.println(sample.name))
  //        outStream.close()
  //      }
  //
  //      val dataContent: Enumerator[Array[Byte]] = Enumerator.fromStream(resultStream)
  //      //    val dataContent = Enumerator("asd","qwe")
  //      Ok.chunked(dataContent.andThen(Enumerator.eof)).withHeaders(
  //        //      http://localhost:8000/gmql-rest/dataSets/asd/downloadZip2
  //        //      CONTENT_TYPE -> "application/zip",
  //        //      CONTENT_DISPOSITION -> s"attachment; filename=MyBasket2.zip; filename*=UTF-8''MyBasket2.zip"
  //      )
  //  }


  //  def getSampleFile(dataSetName: String, file: String) = AuthenticatedAction {
  //    request =>
  //      val username = request.username.get
  //      ut.getTempDir(username)
  //      val directory = ut.getTempDir(username) + "2" + File.separator + dataSetName
  //      //    val fullFile = directory + File.separator + file
  //      Ok.sendFile(new java.io.File(directory, file))
  //  }


  def getUcscLink(datasetName: String) = AuthenticatedAction { implicit request =>
    val username = request.username.get
    //TODO create temp token
    val token = request.authentication.get.authToken
    //      val directory = prepareFile("2", username, dataSetName)
    if (datasetName.startsWith("public."))
      BadRequest("Cannot load public datasets")
    else
      Ok(s"${controllers.gmql.routes.DSManager.getUcscList(datasetName).absoluteURL()}?auth-token=$token")
  }


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
        val trackName: String = sample.name
        val description = trackName
        buf ++= s"""track name=\"$trackName\" description=\"$description\"  useScore=1 visibility=\"3\" $newLine"""
        //      buf ++= s"""track name=\"$trackName\" $newLine"""
        buf ++= s"${controllers.gmql.routes.DSManager.getRegionStream(datasetName, trackName).absoluteURL()}?auth-token=$token $newLine"
      }
      Ok(buf.toString)
    }
  }

  def getMetadata2(fileName: String) = {
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

  //TODO correct temporary directory
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
      for (tuple <- parse2(username, datasetName, parser, sample, optionColumn, 20)) {
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

  def parse2(username: String, datasetName: String, parser: CustomParser, sample: Sample, column: Int, maxRow: Int): Iterator[(String, GValue)] = {
    Logger.info(sample.toString)


    var returnVal = List.empty[(String, GValue)].iterator

    import resource._


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


  def uploadSample(dataSetName: String, schemaName: Option[String]) = AuthenticatedAction(parse.multipartFormData) { request =>
    val username = request.username.get
    Logger.debug("uploadSample => username: " + username)
    //TODO move create empty directory to utils
    val tempDirPath = DatasetUtils.createEmptyTempDirectory(username, dataSetName)


    Logger.debug("uploadSample=>tempDirPath: " + tempDirPath)
    try {
      val files = mutable.Set.empty[String]
      request.body.files.foreach {
        file =>
          val newFile =
            if (file.key == "schema")
              new File(tempDirPath + ".schema")
            else {
              files += tempDirPath + File.separator + file.filename
              new File(tempDirPath + File.separator + file.filename)
            }
          file.ref.moveTo(newFile)
          Logger.info("File: " + file.filename)
      }
      Logger.info("Schema name: " + schemaName.getOrElse("NO INPUT"))

      val schemaPath = tempDirPath + ".schema"
      //      val dataset = IRDataSet(dataSetName, repository.readSchemaFile(schemaPath).fields.map(field => (field.name, field.fieldType)))
      val samples: util.List[GMQLSample] = files.toList.filter(fileName => files.contains(fileName + ".meta")).map(fileName => GMQLSample(fileName, fileName + ".meta"))
      repository.importDs(dataSetName, username, samples, schemaPath)
      Ok("Done")
    }
    catch {
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

  //  def uploadSample2(dataSetName: String, schemaName: Option[String]) = AuthenticatedAction {
  //    request =>
  //      val body = request.body
  //      Logger.debug("body: " + request.body)
  //
  //      Ok("Got: ")
  //  }

  //  def uploadSamplesFromUrls2(dataSetName: String, schemaName: Option[String]) = AuthenticatedAction {
  //    request =>
  //      val body = request.body
  //      Logger.debug("body: " + request.body)
  //      val jsonBody = body.asJson
  //
  //      Logger.debug(body.asJson.getOrElse("YOK").toString)
  //
  //      // Expecting json body
  //      jsonBody.map {
  //        json =>
  //          Ok("Got: " + json.as[String])
  //      }.getOrElse {
  //        BadRequest("Expecting application/json request body")
  //      }
  //
  //  }

  def uploadSamplesFromUrls(dataSetName: String, schemaName: Option[String]) = AuthenticatedAction(parse.json) {
    request =>
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

        Logger.info("Schema name: " + schemaName.getOrElse("NO INPUT"))


        //      val dataset = IRDataSet(dataSetName, repository.readSchemaFile(schemaPath).fields.map(field => (field.name, field.fieldType)))
        val samples: util.List[GMQLSample] = files.toList.filter(fileName => files.contains(fileName + ".meta")).map(fileName => GMQLSample(fileName, fileName + ".meta"))
        repository.importDs(dataSetName, username, samples, schemaPath.getOrElse(""))

        Ok("Files uploaded")
        //      Ok("Got: " + dataFilePaths)
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


  def downloadFile(tempDirPath: String, filePath: String): String = {
    val urlObj = new URL(filePath)

    val fileName = urlObj.getPath.split("/").last
    val newFile = new File(tempDirPath + File.separator + fileName)
    downloadFile(newFile, filePath)
  }

  def downloadFile(newFile: File, filePath: String): String = {
    val urlObj = new URL(filePath)

    //This for getting correct try catch
    urlObj.openStream().close()
    //copy file from url to new file
    val result = urlObj #> newFile !!

    //This will be empty
    Logger.debug("result1:" + result)
    newFile.getAbsolutePath
  }


  //  /**
  //    * call function to copy files from temporary folder to data set folder.
  //    *
  //    * @param username    name of the user
  //    * @param dataSetName name of the data set
  //    * @param schemaName  type of the schema.
  //    */
  //  private def createDataSetFromTemp(username: String, dataSetName: String, schemaName: Option[String]) = {
  //    new DataSetsManager().createDataSet(username, dataSetName, schemaName.getOrElse("UPLOAD"), List.empty.asJava, List.empty.asJava)
  //    //TODO ARIF THIS SHOULD BE DONE IN OTHER PART
  //    //    schemaFilePath.deleteIfExists()
  //  }


  //TODO ARIF remove examples
  def upload1 = Action(parse.multipartFormData) {
    request =>
      //      println("Test" + request.body.files.head.key)
      request.body.file("test").map {
        picture =>

          import java.io.File

          val filename = picture.filename
          //      val contentType = picture.contentType
          picture.ref.moveTo(new File(s"/home/canakoglu/gmql_repository/delete/$filename"))
          Ok("File uploaded")
      }.getOrElse {
        Ok("ERROR")
      }
  }


  ///////////////////////////// TODO DELETE THE FUNCTIONS BELOW

  //TODO ARIF remove
  // DIRECT FILE UPLOAD
  def upload2 = Action(parse.temporaryFile) {
    request =>
      request.body.moveTo(new File("/tmp/picture/uploaded"))
      Ok("File uploaded")
  }


  //TODO ARIF remove
  def upload(dataSetName: String) = Action(parse.multipartFormData) { request =>
    import scalax.file.Path
    Logger.debug("Test " + request.body.files.head.key)
    val path = Path.fromString(s"/home/canakoglu/gmql_repository/delete/$dataSetName/")
    path.deleteRecursively(force = true, continueOnFailure = true)
    path.doCreateParents()
    path.doCreateDirectory()


    request.body.files.foreach {
      file =>
        val filename = file.filename
        val contentType = file.contentType
        val key = file.key
        println(filename + "<==>" + contentType + "<==>" + key)


        file.ref.moveTo(new File(path.path, s"$filename"))
        file.ref.file.delete()
        Ok("File uploaded")
    }

    Ok("finished")
  }


  //TODO
  def addSamples(dataSetName: String) = TODO

  //  /**
  //    * Create temp directory if not exists and returns the directory
  //    *
  //    * @param user username
  //    */
  //  //TODO check if it is using
  //  def checkOrCreateTempDirectory(user: String) {
  //    val tempUserFolderPath = tempFolderRoot + File.separator + user
  //    val d = new File(tempUserFolderPath)
  //    if (!d.exists) d.mkdirs
  //    tempUserFolderPath
  //  }

  //  def prepareFile(id: String, user: String, dataSetName: String) = {
  //    val directory = ut.getTempDir(user) + id + File.separator + dataSetName
  //    val dir = new File(directory)
  //    dir.mkdirs
  //    if (dir.list != null) repository.exportDsToLocal(dataSetName, user, directory)
  //    directory
  //  }
}

