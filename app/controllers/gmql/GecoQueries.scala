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
  def gecoQueriesJson(q_type: String) = cached.status(_ => "query-" + q_type, 200, 3600) { //1 hour
    Action.async {
      val URL = q_type.toLowerCase() match {
        case "federated" => "http://www.bioinformatics.deib.polimi.it/geco_queries/geco_queries_federated.json"
        case _ => "http://www.bioinformatics.deib.polimi.it/geco_queries/geco_queries.json"
      }

      val request = ws.url(URL).get
      Logger.debug("got example query")

      request map { response =>
        Ok(response.json)
      }
    }
  }
}