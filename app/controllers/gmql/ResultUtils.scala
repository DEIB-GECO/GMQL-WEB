package controllers.gmql

import java.io.{File, StringWriter}
import java.util
import javax.ws.rs.core.{MultivaluedMap, Response}
import javax.xml.bind.{JAXBContext, JAXBException, Marshaller}

import com.sun.jersey.core.util.StringKeyObjectValueIgnoreCaseMultivaluedMap
//import orchestrator.entities._
import org.eclipse.persistence.jaxb.MarshallerProperties
import play.api.Logger
import play.api.libs.json.{JsObject, JsString, Json, Writes}
import play.api.mvc.{Controller, RequestHeader, Result, Results}

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
  * Created by canakoglu on 4/11/16.
  */
object ResultUtils extends Controller {


  {
    System.setProperty("javax.xml.bind.context.factory", "org.eclipse.persistence.jaxb.JAXBContextFactory")
    Logger.info("System property set: javax.xml.bind.context.factory=org.eclipse.persistence.jaxb.JAXBContextFactory")
  }


  lazy val NA = MethodNotAllowed("Result cannot be shown, please select 'Accept' header 'application/json' or 'application/xml'")

  def renderedError(statusFunction: Results.Status, error: String = "Error")(implicit request: RequestHeader): Result =
    render {
      case Accepts.Xml() => statusFunction(<error>{error}</error>)
      case Accepts.Json() => statusFunction(JsObject(Seq("error" -> JsString(error))))
      case _ => statusFunction(s"Error: $error")
    }


  def renderedError(statusId: Int, error: String)(implicit request: RequestHeader): Result = renderedError(new Results.Status(statusId), error)



//  /**
//    * returns unauthorized error with the input string
//    *
//    * @param error Error string to return to the user. [[scala.collection.immutable.List]]
//    * @return rendered result by using Accept header by using [[render]]
//    *
//    */
//  def unauthorizedError(error: String)(implicit request: RequestHeader) = renderedError(Results.Unauthorized, error)
//
//
//  /**
//    * returns not found error with the input string
//    *
//    * @param error Error string to return to the user. [[scala.collection.immutable.List]]
//    * @return rendered result by using Accept header by using [[render]]
//    *
//    */
//  def notFoundError(error: String)(implicit request: RequestHeader) = renderedError(Results.NotFound, error)


//  implicit val attributeWrites = new Writes[Attribute] {
//    def writes(attribute: Attribute) = Json.obj(
//      "name" -> attribute.getName
//    )
//  }
//
//  implicit val attributeListWrites = new Writes[AttributeList] {
//    def writes(attributeList: AttributeList) = Json.obj(
//      "attributes" -> attributeList.getAttributes.asScala
//    )
//  }


  //helper class
//  def resultHelper(response: Response): Result =
//    resultHelper(response.getStatus, Option(response.getEntity).getOrElse(""), Option(response.getMetadata).getOrElse(new StringKeyObjectValueIgnoreCaseMultivaluedMap()))
//
//
//  def resultHelper(httpStatus: Int, body: AnyRef = "Ok", headers: MultivaluedMap[String, AnyRef]): Result = httpStatus match {
//    case play.api.http.Status.OK =>
//      var res = body match {
//        case file: File => Results.Ok.sendFile(file, true)
//        case _ => Results.Ok(body.toString)
//      }
//
//      println("body: " + body)
//      val headersMulti: mutable.Map[String, util.List[AnyRef]] = headers.asScala
//      for ((key, values) <- headersMulti; value <- values.asScala) {
//        println("header:" + key + value.toString)
//        res = res.withHeaders(key -> value.toString)
//      }
//      res
//    case _ => Results.Status(httpStatus)("Error code: " + httpStatus + ", body: " + body)
//  }

  /**
    * Convert result from other function to this one
    *
    * @param res response of the other package
    * @return concert result as a string
    */
  //  def unMarshall(res: Response) = {
  //    var jc: JAXBContext = null
  //    val sw: StringWriter = new StringWriter
  //    try {
  //      jc = JAXBContext.newInstance(classOf[AttributeList])
  //      val marshaller: Marshaller = jc.createMarshaller
  //      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
  //      marshaller.marshal(res.getEntity.asInstanceOf[AttributeList], sw)
  //    }
  //    catch {
  //      case e: JAXBException =>
  //        e.printStackTrace()
  //    }
  //    sw.toString
  //  }
//  def unMarshall(res: Response, isJson: Boolean) = {
//    //    unMarshallClass(res, classOf[AttributeList])
//    unMarshallClass(res, res.getEntity.getClass, isJson)
//  }

  //  {
  //        var jc: JAXBContext = null
  //        val sw: StringWriter = new StringWriter
  //        try {
  //          jc = JAXBContext.newInstance(classOf[AttributeList])
  //          val marshaller: Marshaller = jc.createMarshaller
  //          marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
  //          marshaller.marshal(res.getEntity.asInstanceOf[AttributeList], sw)
  //        }
  //        catch {
  //          case e: JAXBException =>
  //            e.printStackTrace()
  //        }
  //        sw.toString
  //      }

  //  def unMarshallJobs(res: Response) = {
  //    var jc: JAXBContext = null
  //    val sw: StringWriter = new StringWriter
  //    try {
  //      jc = JAXBContext.newInstance(classOf[JobList])
  //      val marshaller: Marshaller = jc.createMarshaller
  //      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
  //      marshaller.marshal(res.getEntity, sw)
  //    }
  //    catch {
  //      case e: JAXBException =>
  //        e.printStackTrace()
  //    }
  //    sw.toString
  //  }

//  def unMarshallClass(res: Response, classType: Class[_], isJson: Boolean) = {
//    Logger.info("res.getEntity: " + res.getEntity.toString)
//    Logger.info("classType: " + classType)
//    var jc: JAXBContext = null
//    val sw: StringWriter = new StringWriter
//    try {
//      jc = JAXBContext.newInstance(classType)
//      val marshaller: Marshaller = jc.createMarshaller
//      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
//      if (isJson) {
//        marshaller.setProperty(MarshallerProperties.MEDIA_TYPE, "application/json")
//        //        marshaller.setProperty(MarshallerProperties.JSON_INCLUDE_ROOT, true);
//      }
//      marshaller.marshal(res.getEntity, sw)
//    }
//
//    catch {
//      case e: JAXBException =>
//        e.printStackTrace()
//    }
//    sw.toString
//  }

//  def renderJaxb(response: Response)(implicit request: RequestHeader) = {
//    render {
//      case Accepts.Json() => Ok(ResultUtils.unMarshall(response, true)).as("application/json")
//      case Accepts.Xml() => Ok(ResultUtils.unMarshall(response, false)).as("application/xml")
//    }
//  }

  //  def renderAttributeList(response: Response)(implicit request: RequestHeader) = {
  //    implicit val attributeListWrites = AttributeListHelper.attributeListWrites
  //
  //    val jc = JAXBContext.newInstance(response.getEntity.getClass)
  //
  //    System.setProperty("javax.xml.bind.context.factory", "org.eclipse.persistence.jaxb.JAXBContextFactory")
  //
  //    val marshaller = jc.createMarshaller()
  //    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
  //    marshaller.setProperty(MarshallerProperties.MEDIA_TYPE, "application/json")
  //    marshaller.setProperty(MarshallerProperties.JSON_INCLUDE_ROOT, false);
  //    marshaller.marshal(response.getEntity, System.out)
  //
  //
  //    render {
  //      case Accepts.Json() => Ok(Json.toJson(response.getEntity.asInstanceOf[AttributeList]))
  //      case Accepts.Xml() => Ok(ResultUtils.unMarshall(response, false)).as("text/xml")
  //      //        val result =
  //      //          <dataSetList>
  //      //            {response.getEntity.asInstanceOf[AttributeList].getAttributes.asScala.map(attribute =>
  //      //            <dataSet>
  //      //              <name>
  //      //                {attribute.getName}
  //      //              </name>
  //      //            </dataSet>)}
  //      //          </dataSetList>
  //      //        Ok(result)
  //
  //    }
  //  }
  //
  //  def renderGMQLSchemaCollection(response: Response)(implicit request: RequestHeader) = {
  //    implicit val schemaCollectionWrites = GMQLSchemaCollectionHelper.schemaCollectionWrites
  //
  //    render {
  //      case Accepts.Json() => Ok(Json.toJson(response.getEntity.asInstanceOf[GMQLSchemaCollection]))
  //      case Accepts.Xml() => Ok(ResultUtils.unMarshall(response, false)).as("text/xml")
  //    }
  //  }

//  def findFileKey(file: GMQLFile, fileList: List[Option[String]]): List[String] = {
//    var isResult = false
//    if (file.getFilename == fileList.last.get) {
//      var tempFile = file
//      isResult = true
//      for (el <- fileList.reverse) {
//        if (el.isDefined && el.get != tempFile.getFilename) {
//          isResult = false
//        }
//        tempFile = tempFile.getParent
//        if (tempFile == null) {
//          isResult = false
//        }
//      }
//    }
//
//    if (isResult)
//      file.getKey :: file.getChildren.asScala.toList.flatMap(x => findFileKey(x, fileList))
//    else
//      file.getChildren.asScala.toList.flatMap(x => findFileKey(x, fileList))
//  }

}

//object AttributeListHelper {
//  implicit val attributeWrites = new Writes[Attribute] {
//    def writes(element: Attribute) = Json.obj(
//      "name" -> element.getName
//    )
//  }
//
//  implicit val attributeListWrites = new Writes[AttributeList] {
//    def writes(element: AttributeList) = Json.obj(
//      "attributes" -> element.getAttributes.asScala
//    )
//  }
//}


//object GMQLSchemaCollectionHelper {
//  implicit val SchemaFieldWrites = new Writes[GMQLSchemaField] {
//    def writes(element: GMQLSchemaField) = Json.obj(
//      "name" -> element.getFieldName,
//      "type" -> element.getFieldType
//    )
//  }
//
//
//  implicit val schemaWrites = new Writes[GMQLSchema] {
//    def writes(element: GMQLSchema) = Json.obj(
//      "name" -> element.getSchemaName,
//      "type" -> element.getSchemaType,
//      "fields" -> element.getFields.asScala
//    )
//  }
//
//  implicit val schemaCollectionWrites = new Writes[GMQLSchemaCollection] {
//    def writes(element: GMQLSchemaCollection) = Json.obj(
//      "name" -> element.getCollectionName,
//      "schemas" -> element.getSchemaList.asScala
//    )
//  }
//}

