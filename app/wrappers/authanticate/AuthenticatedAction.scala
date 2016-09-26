package wrappers.authanticate

/**
  *
  * Created by canakoglu on 18-Mar-16.
  *
  * This package is created based on the link below.
  * http://iankent.uk/blog/action-composition-in-play-framework/
  */

import controllers.{SecurityControllerDefaults, SecurityControllerScala}
import models.User
import play.api.mvc.Results.Unauthorized
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object AuthenticatedAction extends AuthenticatedActionBuilder {

  val UnAuthenticatedRequest: String = "UnAuthenticatedRequest"

  def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A]) => Future[Result]) = {
    val res = getAuthenticatedRequest(request)
    res.user match {
      case Some(x) => block(res)
      case None => Future {
        Unauthorized(UnAuthenticatedRequest) //.discardingCookies(DiscardingCookie(SecurityController.AUTH_TOKEN))
      }
    }
  }
}

object AuthenticatedActionMultiple extends AuthenticatedActionBuilder {
  def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A]) => Future[Result]) = {
    val res = getAuthenticatedRequest(request)
    block(res)
  }
}

trait AuthenticatedActionBuilder extends ActionBuilder[AuthenticatedRequest] {
  def getAuthenticatedRequest[A](request: Request[A]) = {
    val user = getUser(request)
    user match {
      case Some(_) => AuthenticatedRequest[A](user, request)
      case None => AuthenticatedRequest[A](None, request)
    }
  }

  def getUser[A](request: Request[A]) = {
    lazy val fromCookie: String = request.cookies.get(SecurityControllerDefaults.AUTH_TOKEN_COOKIE).getOrElse(new Cookie(name = SecurityControllerDefaults.AUTH_TOKEN_COOKIE, value = null)).value

    val token = request.headers.get(SecurityControllerDefaults.AUTH_TOKEN_HEADER) match {
      case Some(token) => token
      case None =>
        request.queryString.get(SecurityControllerDefaults.QUERY_AUTH_TOKEN) match {
          case Some(token) => Some(token(0)).getOrElse(fromCookie)
          case None => fromCookie
        }
    }
    Option(User.findByAuthToken(token))
  }

}





