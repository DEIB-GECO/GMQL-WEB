package controllers.gmql


import javax.inject.Inject

import play.api.Logger
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, Controller}

import scala.concurrent.ExecutionContext
import play.api.cache.Cached

import scala.concurrent.duration._


class GecoQueries @Inject()(ws: WSClient, cached: Cached)(implicit ec: ExecutionContext) extends Controller {

  def gecoQueries = Action { implicit request =>
    Ok(views.html.geco_queries.table())
  }

  //TODO add cache here
  def gecoQueriesJson = //cached.status(_ => "homePage", 200, 3600) { //ten minutes
    Action.async {
      val request = ws.url("http://www.bioinformatics.deib.polimi.it/geco_queries/geco_queries.json").get
      Logger.debug("got example query")

      request map { response =>
        Ok(response.json)
      }
    }
  //}
}