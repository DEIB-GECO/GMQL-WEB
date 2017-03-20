package controllers
import controllers.gmql.SecurityControllerDefaults._

import scala.util.matching.Regex

/**
  * Created by canakoglu on 7/21/16.
  */
object Validation {
  val preUsername = "Invalid username. "
  val preFilename = "Invalid query name. "
  private val emailRegex: Regex = """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""".r

  def validateUsername(username: String) = {
    if (username.startsWith(GUEST_USER))
      Some(s"${preUsername}It is forbidden to start with ${GUEST_USER}")
    else if (username.startsWith(PUBLIC_USER) && !play.Play.isDev)
      Some(s"${preUsername}It is forbidden to start with ${PUBLIC_USER}")
    else
      alphaNumerical(username, preUsername)
  }

  def validateFilename(filename: String) =    alphaNumerical(filename, preFilename)

  def validateEmail(email: String) = {
    emailRegex.findFirstIn(email) match {
      case Some(s) => None
      case None => Some("Please use correct email")
    }
  }

  def nonEmpty(text: String, pre: String) = {
    if (!text.matches("^.+$"))
      Some(s"${pre} cannot be empty")
    else
      None
  }

  def alphaNumerical(text: String, pre: String = "") = {
    if(text.isEmpty)
      Some(s"${pre}It cannot be empty")
    else if (!text.matches("^[a-zA-Z0-9_]+$"))
      Some(s"${pre}Use only alphanumerical characters or underscore")
    else
      None
  }
}
