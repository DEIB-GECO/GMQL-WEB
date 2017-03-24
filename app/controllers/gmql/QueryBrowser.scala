package controllers.gmql

import javax.inject.Singleton

import io.swagger.annotations._
import models.{QueryDao, QueryModel}
import play.api.libs.json.Json
import play.api.mvc.Controller
import wrappers.authanticate.AuthenticatedAction

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}


/**
  * Created by canakoglu on 3/1/17.
  */
@Singleton
@Api(value = SwaggerUtils.swaggerQueryBrowser, produces = "application/json, application/xml")
class QueryBrowser extends Controller {

  @ApiImplicitParams(Array(new ApiImplicitParam(name = "X-AUTH-TOKEN", dataType = "string", paramType = "header", required = true)))
  def getQueries() = AuthenticatedAction.async { implicit request =>
    val user = request.user.get
    val futureQuerySeq: Future[Seq[QueryModel]] = QueryDao.getUserQueries(user.id.get)

    val futureQueries: Future[Queries] = futureQuerySeq.map(queries => Queries(queries.map(query => Query(query.name, Some(query.text)))))

    futureQueries.map { queries =>
      render {
        case Accepts.Json() => Ok(Json.toJson(queries))
        case Accepts.Xml() => Ok(queries.getXml)
      }
    }
  }

  @ApiImplicitParams(Array(new ApiImplicitParam(name = "X-AUTH-TOKEN", dataType = "string", paramType = "header", required = true)))
  def getQuery(queryName: String) = AuthenticatedAction.async { implicit request =>
    val user = request.user.get
    val futureQuerySeq = QueryDao.getUserQuery(user.id.get, queryName)

    val futureQuery: Future[Option[Query]] = futureQuerySeq.map(_.map(query => Query(query.name, Some(query.text))))

    futureQuery.map {
      case Some(query) => render {
        case Accepts.Json() => Ok(Json.toJson(query))
        case Accepts.Xml() => Ok(query.getXml)
      }
      case None => NotFound("Query Not Found")
    }
  }

  @ApiOperation(value = "Save query",
    notes = "Save query, if the query exists then update or save as a new query.",
    consumes = "text/plain"
  )
  @ApiImplicitParams(Array(new ApiImplicitParam(name = "body", dataType = "string", paramType = "body"),
  new ApiImplicitParam(name = "X-AUTH-TOKEN", dataType = "string", paramType = "header", required = true)))
  def saveQuery(queryName: String) = AuthenticatedAction { request =>
    val username = request.username.getOrElse("")
    val user = request.user.get
    val newQueryText = request.body.asText.getOrElse("")

    val queryOption: Option[QueryModel] = Await.result(QueryDao.getUserQuery(user.id.get, queryName), Duration.Inf)

    queryOption match {
      case Some(query) =>
        if (newQueryText != query.text)
          QueryDao.updateQueryText(query.id.get, newQueryText)
      case None => QueryDao.add(QueryModel(user.id.get, queryName, newQueryText))
    }
    Ok("Saved")
  }
}
