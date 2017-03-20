package controllers

import javax.inject.{Inject, Singleton}

import controllers.gmql.ResultUtils.{NA, renderedError}
import controllers.gmql.{Dataset, DatasetUtils}
import play.api.libs.ws.WSClient
import play.api.mvc._
import io.swagger.annotations._
import it.polimi.genomics.repository.GMQLExceptions.GMQLDSNotFound
import play.api.libs.json.Json
import wrappers.authanticate.AuthenticatedAction


@Singleton
class Application @Inject()(ws: WSClient) extends Controller {


  def zip = Action {
    import play.api.libs.iteratee._
    import java.util.zip._
    import scala.concurrent.ExecutionContext.Implicits.global

    val r = new java.util.Random()

    val enumerator: Enumerator[Array[Byte]] = Enumerator.outputStream { os =>
      val zip = new ZipOutputStream(os);
      Range(0, 100).map { i =>
        zip.putNextEntry(new ZipEntry("test-zip/README-"+i+".txt"))
        zip.write("Here are 100000 random numbers:\n".map(_.toByte).toArray)
        // Let's do 100 writes of 1'000 numbers
        Range(0, 100).map { j =>
          zip.write((Range(0, 1000).map(a=>j*100000 + a).map(_.toString).mkString("\n")).map(_.toByte).toArray);
          zip.write("\n".map(_.toByte).toArray)
        }
        zip.closeEntry()
      }
      zip.close()
    }
    Ok.stream(enumerator >>> Enumerator.eof).withHeaders(
      "Content-Type"->"application/zip",
      "Content-Disposition"->"attachment; filename=test.zip"
    )
  }

//  def index = Action { implicit request =>
//    Ok(views.html.index("Your new application is ready."))
//  }
//
//
//  def ajaxCall = Action { implicit request =>
//    Ok("Ajax Call!")
//  }

  def heatMapTest = Action{implicit request =>
    Ok(views.html.heat_map(List("1","2","3","4"),2,2,List("row1","row2"),List("col1","col2")))
//    Ok("")
  }

  def gmqlRestHelp = Action { implicit request =>
    Ok(views.html.gmql_rest_help_main())
  }


  def gmqlHelp = Action { implicit request =>
    Ok(views.html.gmql_help_main())
  }


  def sampleMetadata (totalCount:Int, list:String)= Action {
    val ls = list.split("_").map(_.toInt)
    Ok(views.html.gmql.gmql_metadata_sample(totalCount, ls))
  }


  def gmql = Action { implicit request =>
    Ok(views.html.gmql_main())
  }


  def helpInner = Action { request =>
    val asd = Map(""->"",""->"")
    asd.head._1
    //    Ok(views.html.help_inner("Your new application is ready."))
    Redirect("/dataSet/all") //.withHeaders(SecurityController.AUTH_TOKEN_HEADER -> "test-best-token")
    //    (new DSManager).dataSetAll.apply(request)
  }

  //  def helpRedirect: Action[AnyContent] = Action.async { (request: Request[AnyContent]) =>
  //    val asd = request.cookies.get("auth-token").get.value
  //    println("asd: " + asd)
  //    request.headers.add(SecurityController.AUTH_TOKEN_HEADER -> asd)
  //    //      get(SecurityController.AUTH_TOKEN_HEADER)
  //    //    println("qwe: "  + qwe)
  //    //
  //    //    val dsManager = new DSManager
  //    ////    Redirect(controllers.gmql.routes.DSManager.dataSetAll).withHeaders(SecurityController.AUTH_TOKEN_HEADER -> asd)
  //    //        Redirect(routes.Application.helpRedirect()).withHeaders(SecurityController.AUTH_TOKEN_HEADER -> asd)
  //    //
  //
  //    println(request.headers.get(SecurityController.AUTH_TOKEN_HEADER))
  //
  //
  //    val rhWithTraceId: RequestHeader = request.copy(tags = request.tags + (SecurityController.AUTH_TOKEN_HEADER -> asd))
  //
  //
  //    val res = (new DSManager).dataSetAll.apply(request)
  //
  //    res
  //    //    Future{Ok()}
  //
  //    //    val request2: WSRequest = ws.url(controllers.gmql.routes.DSManager.dataSetAll.url)
  //    //    val complexRequest: WSRequest =
  //    //      request2.withHeaders(SecurityController.AUTH_TOKEN_HEADER -> asd)
  //    //
  //    //    val futureResponse: Future[WSResponse] = complexRequest.get()
  //    //
  //    //    futureResponse.map{ response =>
  //    //      Ok(response.body)
  //    //    }
  //
  //
  //  }

  @ApiResponses(Array(
    new ApiResponse(code = 400, message = "Invalid ID supplied"),
    new ApiResponse(code = 404, message = "Pet not found")))
  def getPetById(
                  @ApiParam(value = "ID of the pet to fetch") id: String) = Action {
    implicit request =>
      Ok("hello - " + id)
  }


  @ApiModelProperty(hidden = true)
  def swagger = Action {
    request =>
      Ok(views.html.swagger())
  }


}