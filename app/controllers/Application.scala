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

  def heatMapTest = Action{implicit request =>
    Ok(views.html.heat_map(List("1","2","3","4"),2,2,List("row1","row2"),List("col1","col2")))
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

  @ApiModelProperty(hidden = true)
  def swagger = Action {
    request =>
      Ok(views.html.swagger())
  }

  def gecoQueries = Action { implicit request =>
      Ok(views.html.geco_queries.table())
  }


}