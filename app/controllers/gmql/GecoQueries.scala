package controllers.gmql


import javax.inject.Inject

import play.api.libs.ws.WSClient
import play.api.mvc.{Action, Controller}

import scala.concurrent.ExecutionContext

class GecoQueries @Inject() (ws: WSClient)(implicit ec: ExecutionContext) extends Controller {

  def gecoQueries = Action { implicit request =>
    Ok(views.html.geco_queries.table())
  }

  //TODO add cache here
  def gecoQueriesJson = Action.async {
    val request = ws.url("http://www.bioinformatics.deib.polimi.it/geco_queries/geco_queries.json").get

    request map { response =>
      Ok(response.json)
    }

  }
}