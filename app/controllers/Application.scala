package controllers

import javax.inject.{Inject, Singleton}

import play.api.libs.ws.WSClient
import play.api.mvc._

@Singleton
class Application @Inject()(ws: WSClient) extends Controller {

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

  def help = Action { implicit request =>
    Ok(views.html.help_main())
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
  //    val asd = request.cookies.get("authToken").get.value
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


}