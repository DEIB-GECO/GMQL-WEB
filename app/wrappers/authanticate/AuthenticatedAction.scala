package wrappers.authanticate

/**
  *
  * Created by canakoglu on 18-Mar-16.
  *
  * This package is created based on the link below.
  * http://iankent.uk/blog/action-composition-in-play-framework/
  */

import controllers.gmql.SecurityControllerDefaults._
import controllers.{Default, SecurityController}
import it.polimi.genomics.core.GDMSUserClass
import it.polimi.genomics.repository.federated.communication.NameServer
import models.{AuthenticationDao, AuthenticationModel, UserDao, UserModel}
import play.api.http.MimeTypes
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc.Results.Unauthorized
import play.api.mvc._
import utils.GmqlGlobal

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}


object AuthenticatedAction extends AuthenticatedActionBuilder {

  val UnAuthenticatedRequest: String = "UnAuthenticatedRequest"

  def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A]) => Future[Result]) = {
    val res = getAuthenticatedRequest(request)
    res.user match {
      case Some(_) => block(res)
      case None =>
        Future {
          val AcceptsXml = Accepting("application/xml")
          val AcceptsJson = Accepting(MimeTypes.JSON)
          println("Hello" + request.acceptedTypes)

          Default.render {
            case Default.Accepts.Xml() => Unauthorized(<error>{UnAuthenticatedRequest}</error>)
            case Default.Accepts.Json() => Unauthorized(Json.parse("{\"error\" : \"" + UnAuthenticatedRequest + "\"}"))
            case _ => Unauthorized(UnAuthenticatedRequest)
          }(request) //.discardingCookies(DiscardingCookie(SecurityController.AUTH_TOKEN))
        }
    }
  }
}

//object AuthenticatedActionMultiple extends AuthenticatedActionBuilder {
//  def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A]) => Future[Result]) = {
//    val res = getAuthenticatedRequest(request)
//    block(res)
//  }
//}

trait AuthenticatedActionBuilder extends ActionBuilder[AuthenticatedRequest] {
  def getAuthenticatedRequest[A](request: Request[A]): AuthenticatedRequest[A] = {
    val userAuth = getUserAuthentication(request)
    userAuth match {
      case Some((user, authentication)) => AuthenticatedRequest[A](Some(user), Some(authentication), request)
      case None => AuthenticatedRequest[A](None, None, request)
    }
  }

  def getUserAuthentication[A](request: Request[A]): Option[(UserModel, AuthenticationModel)] = {
    lazy val fromCookie: String = request.cookies.get(AUTH_TOKEN_COOKIE).getOrElse(Cookie(name = AUTH_TOKEN_COOKIE, value = null)).value

    val token = request.headers.get(AUTH_TOKEN_HEADER) match {
      case Some(token) => token
      case None =>
        request.queryString.get(QUERY_AUTH_TOKEN) match {
          case Some(token) => Some(token(0)).getOrElse(fromCookie)
          case None => fromCookie
        }
    }
    //    Option(User.findByAuthToken(token))
    if (token == null)
      None
    else {
      if (token == "FEDERATED-TOKEN") {
        val instanceName = request.headers.get(AUTH_HEADER_NAME_FN) match {
          case Some(token) => token
          case None =>
            request.queryString.get(AUTH_HEADER_NAME_FN) match {
              case Some(token) => Some(token(0)).getOrElse(request.cookies.get(AUTH_HEADER_NAME_FN).getOrElse(Cookie(name = AUTH_HEADER_NAME_FN, value = null)).value)
              case None => fromCookie
            }
        }
        println("instanceName: '" + instanceName + "'")
        val instanceToken = request.headers.get(AUTH_HEADER_NAME_FT) match {
          case Some(token) => token
          case None =>
            request.queryString.get(AUTH_HEADER_NAME_FT) match {
              case Some(token) => Some(token(0)).getOrElse(request.cookies.get(AUTH_HEADER_NAME_FT).getOrElse(Cookie(name = AUTH_HEADER_NAME_FT, value = null)).value)
              case None => fromCookie
            }
        }
        println("instanceToken: '" + instanceToken + "'")

        val isValid = new NameServer().validateToken(instanceName, instanceToken)
        println("isValid: " + isValid)

        if(isValid) {
          if (Await.result(UserDao.getByUsername("federated"), Duration.Inf).isEmpty) {
            //TODO possibly add FEDERATED user type
            GmqlGlobal.repository.registerUser("federated")
            val userId = Await.result(UserDao.add(UserModel("federated", GDMSUserClass.BASIC, "", SecurityController.getSha512("FEDERATED-TOKEN"), "Fede", "Rated")), Duration.Inf)
            Await.result(AuthenticationDao.add(AuthenticationModel(userId.get, None, "FEDERATED-TOKEN")), Duration.Inf)
          }
          val asd = AuthenticationDao.getByToken(token)
          Await.result(asd, Duration.Inf)
        }
        else
          None
      }
      else {
        val asd = AuthenticationDao.getByToken(token)
        Await.result(asd, Duration.Inf)
      }
    }
    //    Some(User2(1,"","canakoglu","",null,"","",null,null,false))
    //    val test = asd.value
    //    var res :Option[User2] = None
    //    val qwe=asd.onComplete {
    //      case Success(temp) =>
    //        res = temp
    //      case Failure(ex) =>
    //        throw ex
    //    }()
    //    res
  }

}





