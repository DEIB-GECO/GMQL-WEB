package wrappers.authanticate

import models.{AuthenticationModel, UserModel}
import play.api.mvc.Request

/**
  * Created by canakoglu on 18-Mar-16.
  */
trait AuthenticatedRequest[+A] extends Request[A] {
  val user: Option[UserModel]
  val username: Option[String]
  val authentication: Option[AuthenticationModel]
}

object AuthenticatedRequest {
  def apply[A](u: Option[UserModel], a: Option[AuthenticationModel], request: Request[A]) = new AuthenticatedRequest[A] {
    //@formatter:off
    override def id = request.id
    override def tags = request.tags
    override def body = request.body
    override def headers = request.headers
    override def queryString = request.queryString
    override def path = request.path
    override def uri = request.uri
    override def method = request.method
    override def version = request.version
    override def remoteAddress = request.remoteAddress
    override def secure = request.secure
    override val user = u
    override val username = u match {
      case Some(user) => Option(user.username)
      case None => None
    }
    override val authentication = a
    //@formatter:on
  }
}