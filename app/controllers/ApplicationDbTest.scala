package controllers

import javax.inject.{Inject, Singleton}

import it.polimi.genomics.repository.GMQLRepository
import models.{AuthenticationDao, UserDao}
import play.api.db.slick.DatabaseConfigProvider
import play.api.mvc._
import slick.driver.JdbcProfile
import utils.GmqlGlobal
import wrappers.authanticate.AuthenticatedAction

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global


//class Application2 @Inject()(dbConfigProvider: DatabaseConfigProvider) extends Controller {
//  val dbConfig = dbConfigProvider.get[JdbcProfile]
//}
//@Singleton
//class HomeController @Inject() (userDAO: UserDAO)
//                               (implicit ec: ExecutionContext)
//  extends Controller {
//
//  def index = Action.async { implicit request =>
//    userDAO.all.map { users =>
//      Ok(views.html.index(users))
//    }
//  }
//
//}


@Singleton
class Application2 @Inject()(dbConfigProvider: DatabaseConfigProvider) extends Controller {
  val dbConfig = dbConfigProvider.get[JdbcProfile]

  val repository: GMQLRepository = GmqlGlobal.repository
  val ut = GmqlGlobal.ut


  def hello1() = Action.async { implicit request =>
    AuthenticationDao.getByToken("test-best-token").map(_.map(_._1.username).getOrElse("unfound")).map(Ok(_))
  }

  def hello2() = Action.async { implicit request =>
    UserDao.get(33).map(_.map(_.username).getOrElse("unfound")).map(Ok(_))
  }

  def hello3() = Action { implicit request =>
    Ok(repository.getMeta("asd", "canakoglu"))
  }


  def hello4(datasetName: String, sampleName: String) = AuthenticatedAction { implicit request =>
    var username: String = request.username.get
    var dsName = datasetName
    // if public then user name is public and get the correct dataset name
    if (datasetName.startsWith("public.")) {
      username = "public"
      dsName = dsName.substring("public.".length)
    }
    val sample = repository.listDSSamples(dsName, username).find(_.name.split("\\.").head.endsWith(sampleName))

    Ok(repository.getSampleMeta("asd", "canakoglu", sample.get))
  }


  def hello5() = TODO

  //    Action { implicit request =>
  //    Ok(repository.getMeta("asd","canakoglu"))
  //  }
}