package wrappers.authanticate

import models.User
import play.api.mvc.Request

/**
  * Created by canakoglu on 18-Mar-16.
  */
trait AuthenticatedRequest[+A] extends Request[A] {
  val user: Option[User]
  val username: Option[String]
}

object AuthenticatedRequest {
  def apply[A](u: Option[User], request: Request[A]) = new AuthenticatedRequest[A] {
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
      case Some(user) => Option(user.getUsername)
      case None => None
    }
    //@formatter:on
  }
}