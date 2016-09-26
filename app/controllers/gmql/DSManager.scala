package controllers.gmql

import java.io.{File, FileNotFoundException, IOException}
import java.net.URL
import javax.inject.Singleton
import javax.swing.filechooser.FileNameExtensionFilter
import javax.ws.rs.core.Response

import gql.services.rest.DataSetsManager
import it.polimi.genomics.core.{GNull, _}
import it.polimi.genomics.spark.implementation.loaders.CustomParser
import org.xml.sax.SAXException
import play.api.Logger
import play.api.mvc.{Action, Controller}
import wrappers.authanticate.AuthenticatedAction

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer
import scala.io.Source
import scala.sys.process._
import scalax.file.Path


/**
  * Created by Canakoglu on 15-Mar-16.
  */
@Singleton
class DSManager extends Controller {


  /**
    *
    * @return
    */
  def dataSetAll = AuthenticatedAction { implicit request =>
    //    val host = "http://" + request.headers.get("host").getOrElse("")
    val response: Response = new DataSetsManager().listAllDataSets(request.username.getOrElse(""))
    ResultUtils.renderJaxb(response)
  }

  /**
    *
    * @param dataSetName name of the data set
    * @return
    */

  def dataSetSamples(dataSetName: String) = AuthenticatedAction { implicit request =>
    //    try {
    val response = new DataSetsManager().listDataSetSamples(dataSetName, request.username.getOrElse(""))
    ResultUtils.renderJaxb(response)
    //      val resAsString = ResultUtils.unMarshall(response)
    //      Ok(resAsString).as("text/xml")
    //    } catch {
    //      case e: NullPointerException => NotFound("No data set found")
    //    }
  }


  def dataSetDeletion(dataSetName: String) = AuthenticatedAction { request =>
    val response = new DataSetsManager().deleteDataSet(request.username.getOrElse(""), dataSetName)
    ResultUtils.resultHelper(response)
  }

  def zipFilePreparation(dataSetName: String, clean: String) = AuthenticatedAction { request =>
    val username = request.username.getOrElse("")
    val response = new DataSetsManager().prepareFileZip(username, dataSetName, clean)
    ResultUtils.resultHelper(response)
  }


  def downloadFileZip(dataSetName: String) = AuthenticatedAction { request =>
    val username = request.username.getOrElse("")
    new DataSetsManager().prepareFileZip(username, dataSetName, "false")
    val response = new DataSetsManager().downloadFileZip(username, dataSetName)
    val res = ResultUtils.resultHelper(response)
    res
  }


  def getSampleFile(dataSetName: String, file: String) = AuthenticatedAction { request =>
    val username = request.username.get
    val directory = DataSetsManager.tempFolderRoot + "2" + File.separator + username + File.separator + dataSetName
    //    val fullFile = directory + File.separator + file
    Ok.sendFile(new java.io.File(directory, file))
  }

  val newLine = sys.props("line.separator")


  def getUcscLink(dataSetName: String) = AuthenticatedAction { implicit request =>
    val username = request.username.get
    //TODO create temp token
    val token = request.user.get.authToken
    val directory = DataSetsManager.prepareFile("2", username, dataSetName)


    Ok(s"${controllers.gmql.routes.DSManager.getUcscList(dataSetName).absoluteURL()}?auth-token=$token $newLine")
  }


  def getUcscList(dataSetName: String) = AuthenticatedAction { implicit request =>
    // http://genome.ucsc.edu/cgi-bin/hgTracks?org=human&hgt.customText=http://genomic.elet.polimi.it/gmql-rest/dataSet/heatmap/parse2?auth-token=test-best-token
    // http://genome.ucsc.edu/cgi-bin/hgTracks?org=human&hgt.customText=http://www.bioinformatics.deib.polimi.it/canakoglu/test2.txt
    // val username = request.username.getOrElse("")
    val username = request.username.get
    val directory = DataSetsManager.prepareFile("2", username, dataSetName)
    val fileList = new File(directory).listFiles.filter(file => !file.getName.endsWith(".gmql") && !file.getName.endsWith(".meta") && !file.getName.endsWith(".crc") && !file.getName.endsWith(".schema")).map(_.getName)

    val buf = new StringBuilder
    //TODO create temp token
    val token = request.user.get.authToken

    //    buf ++= "browser position chr1:270-1100"
    for (file <- fileList) {
      //      Logger.debug("file asd:" + file)
      val trackName = file.split("\\.").head
      val description = getMetadata(trackName)
      buf ++= s"""track name=\"$trackName\" description=\"$description\"  useScore=1 visibility=\"3\" $newLine"""
      //      buf ++= s"""track name=\"$trackName\" $newLine"""
      buf ++= s"${controllers.gmql.routes.DSManager.getSampleFile(dataSetName, file).absoluteURL()}?auth-token=$token $newLine"
    }
    Ok(buf.toString)
  }

  def getMetadata(fileName: String) = {
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


  def parseFiles(dataSetName: String, columnName: String) = AuthenticatedAction { implicit request =>
    val username = request.username.getOrElse("")
    val directory = DataSetsManager.prepareFile("2", username, dataSetName)
    import it.polimi.genomics.spark.implementation.loaders._
    val isCount = columnName.startsWith("count_")


    val parser = new CustomParser()
    parser.setSchema(dataSetName, username)
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
    val fileList = new File(directory).listFiles.filter(file => !file.getName.endsWith(".gmql") && !file.getName.endsWith(".meta") && !file.getName.endsWith(".crc") && !file.getName.endsWith(".schema")).map(_.getCanonicalPath)

    Logger.debug("after file list")
    val wholeListBuffer = fileList.map { file =>
      Logger.debug("file:" + file)
      val tempList: ListBuffer[GValue] = ListBuffer[GValue]()
      for (tuple <- parse2(parser, file, optionColumn, 1000)) {
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

    var columnNames = (for (file <- fileList)
      yield getMetadata(file.split(File.separator).last.split("\\.").head)).toList
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
    Ok(views.html.heat_map(result.toList, rowLength, columnLength, rowNames, columnNames.toList))
  }

  def parse2(parser: CustomParser, fileName: String, column: Int, maxRow: Int): Iterator[(String, GValue)] = {
    Logger.warn(fileName)


    var returnVal = List.empty[(String, GValue)].iterator

    import resource._


    //    http://jsuereth.com/scala-arm/usage.html
    for {
      file <- managed(Source.fromFile(fileName))
    } {
      val fileLines = file.getLines

      val res2 = new ListBuffer[(String, GValue)]

      var count = 0
      while (fileLines.hasNext && count < maxRow) {
        val line = fileLines.next
        val parsed: Option[(GRecordKey, Array[GValue])] = parser.region_parser((999, line))

        val temp = if (parsed.isDefined) {
          val getParsed = parsed.get
          val getParsed1 = getParsed._1
          val getParsed2: Array[GValue] = getParsed._2
          //        Logger.debug("getParsed1" + getParsed1)
          (s"${getParsed1._2}_${getParsed1._3}_${getParsed1._4}", getParsed2(column))
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
    val tempDirPath = createEmptyTempDirectory(username, dataSetName)


    Logger.debug("uploadSample=>tempDirPath: " + tempDirPath)
    try {
      request.body.files.foreach { file =>
        val newFile =
          if (file.key == "schema")
            new File(tempDirPath + ".schema")
          else
            new File(tempDirPath + File.separator + file.filename)

        file.ref.moveTo(newFile)
        Logger.info("File: " + file.filename)
      }
      Logger.info("Schema: " + schemaName.getOrElse("NO INPUT"))

      val response = createDataSetFromTemp(username, dataSetName, schemaName)
      ResultUtils.resultHelper(response)
    }
    catch {
      case e: Exception =>
        val regionsDirPath = Path.fromString(tempDirPath)
        if (regionsDirPath.exists || regionsDirPath.isDirectory) {
          try {
            regionsDirPath.deleteRecursively(force = true, continueOnFailure = true)
          }
          catch {
            case ex: IOException =>
              Logger.error("Error", ex)
          }
        }
        val message =
          if (e.isInstanceOf[SAXException])
            "The dataset schema does not confirm the schema style (XSD) \n" + e.getMessage
          else {
            Logger.error("Upload Error", e)
            "Unknown error"  +  {if(play.Play.isDev) ": " + e.getMessage else ""}
          }
        BadRequest(message)
    }


  }

  def uploadSample2(dataSetName: String, schemaName: Option[String]) = AuthenticatedAction { request =>
    val body = request.body
    Logger.debug("body: " + request.body)

    Ok("Got: ")

  }

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

  def uploadSamplesFromUrls(dataSetName: String, schemaName: Option[String]) = AuthenticatedAction(parse.json) { request =>
    val username = request.username.get
    val dataFilePaths = (request.body \ "data_files").asOpt[Seq[String]].getOrElse(Seq.empty[String])
    val schemaFileOpt = (request.body \ "schema_file").asOpt[String]

    val tempDirPath = createEmptyTempDirectory(username: String, dataSetName: String)

    try {
      dataFilePaths.foreach { filePath =>
        downloadFile(tempDirPath, filePath)
      }
      schemaFileOpt.map(schemaFilePath =>
        downloadFile(new File(tempDirPath + ".schema"), schemaFilePath)
      )



      createDataSetFromTemp(username, dataSetName, schemaName)
      Ok("Files uploaded")
      //      Ok("Got: " + dataFilePaths)
    } catch {
      case e: FileNotFoundException =>
        Logger.error("error", e)
        BadRequest("File not found: " + e.getMessage + ". Please check the URLs and call the service again")
    }

  }


  def downloadFile(tempDirPath: String, filePath: String) {
    val urlObj = new URL(filePath)

    val fileName = urlObj.getPath.split("/").last
    val newFile = new File(tempDirPath + File.separator + fileName)
    downloadFile(newFile, filePath)

  }

  def downloadFile(newFile: File, filePath: String) {
    val urlObj = new URL(filePath)


    //This for getting correct try catch
    urlObj.openStream().close()
    //copy file from url to new file
    val result = urlObj #> newFile !!

    //This will be empty
    Logger.debug("result1:" + result)
  }


  /**
    * Create or clean temp folder for the data set.
    *
    * @param username    name of the user
    * @param dataSetName name of the data set
    * @return full path to the temporary directory
    */
  private def createEmptyTempDirectory(username: String, dataSetName: String) = {
    val tempDirPath = DataSetsManager.tempFolderRoot + File.separator + username + File.separator + "upload" + File.separator + dataSetName
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

  /**
    * call function to copy files from temporary folder to data set folder.
    *
    * @param username    name of the user
    * @param dataSetName name of the data set
    * @param schemaName  type of the schema.
    */
  private def createDataSetFromTemp(username: String, dataSetName: String, schemaName: Option[String]) = {
    new DataSetsManager().createDataSet(username, dataSetName, schemaName.getOrElse("UPLOAD"), List.empty.asJava, List.empty.asJava)
    //TODO ARIF THIS SHOULD BE DONE IN OTHER PART
    //    schemaFilePath.deleteIfExists()
  }


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
  def upload(dataSetName: String) = Action(parse.multipartFormData) {
    request =>
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
}

