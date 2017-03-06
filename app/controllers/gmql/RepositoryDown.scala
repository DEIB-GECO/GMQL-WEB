//package controllers.gmql
//
//import javax.inject.Singleton
//
//import gql.services.rest.RepositoryDownloader
//import play.api.mvc.Controller
//import wrappers.authanticate.AuthenticatedAction
//
//import scala.collection.JavaConversions._
//
///**
//  * Created by canakoglu on 4/12/16.
//  */
//@Singleton
//class RepositoryDown extends Controller {
//
//  def downloadFile(fileKey: String) = AuthenticatedAction { request =>
//    val username = request.username.getOrElse("")
//    val response = new RepositoryDownloader().downloadFile(username, fileKey)
//    val resAsString = ResultUtils.unMarshall(response, false)
//    Ok(resAsString).as("text/xml")
//  }
//
//  def downloadFileZip(filename: Option[String], fileKeys: Option[List[String]]) = AuthenticatedAction { request =>
//    //    val javaList = seqAsJavaList(fileKeys.getOrElse(Seq()))
//    val username = request.username.getOrElse("")
//    val response = new RepositoryDownloader().downloadFileZip(username, filename.get, fileKeys.get)
//    val resAsString = ResultUtils.unMarshall(response, false)
//    Ok(resAsString).as("text/xml")
//  }
//
//
//}
