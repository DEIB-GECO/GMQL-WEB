package controllers

import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.{Inject, Singleton}

import it.polimi.genomics.repository.GMQLRepository
import models.{AuthenticationDao, AuthenticationModel, UserDao, UserModel}
import play.api.Play.current
import play.api.libs.json._
import play.api.libs.mailer._
import play.api.mvc._
import play.api.{Logger, Play}
import utils.GmqlGlobal
import wrappers.authanticate.AuthenticatedAction

import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object SecurityControllerDefaults {

  val AUTH_TOKEN_HEADER: String = "X-Auth-Token"
  val AUTH_TOKEN_COOKIE: String = "authToken"
  val QUERY_AUTH_TOKEN: String = "auth-token"
  val GUEST_USER: String = "guest_new"
  val PUBLIC_USER: String = "public"
}

/**
  * Created by canakoglu on 6/13/16.
  */
@Singleton
class SecurityController @Inject()(mailerClient: MailerClient) extends Controller {
  val repository: GMQLRepository = GmqlGlobal.repository
  val ut = GmqlGlobal.ut

  private val guestCounter: AtomicInteger = new AtomicInteger

  case class UserData(username: String, password: String, email: String, firstName: String, lastName: String)

  def registerUser = Action(parse.json) { implicit request =>

    //    val userForm = Form(
    //      mapping(
    //        "username" -> nonEmptyText,
    //        "password" -> email,
    //        "email" -> text,
    //        "firstName" -> text,
    //        "lastName" -> text
    //      )(UserData.apply)(UserData.unapply)
    //    )
    //
    //
    ////    val userData = userForm.bind(request.body).get
    //    val userData2 = userForm.bindFromRequest
    //    if(userData2.hasErrors)
    //      Logger.error(userData2.errors.toString())
    //
    //    val asd: ValidationResult = Constraints.emailAddress.apply("")
    //    asd match {
    //      case Valid =>
    //      case Invalid(errors: Seq[ValidationError]) =>
    //        Logger.error(errors.toString())
    //    }

    val errorList = ListBuffer.empty[Option[String]]
    val username = (request.body \ "username").asOpt[String].getOrElse("")
    val password = (request.body \ "password").asOpt[String].getOrElse("")
    val email = (request.body \ "email").asOpt[String].getOrElse("")
    val firstName = (request.body \ "firstName").asOpt[String].getOrElse("")
    val lastName = (request.body \ "lastName").asOpt[String].getOrElse("")

    errorList += Validation.validateUsername(username)
    errorList += Validation.nonEmpty(password, "Password")
    errorList += Validation.validateEmail(email)
    errorList += Validation.nonEmpty(firstName, "First name")
    errorList += Validation.nonEmpty(lastName, "Last name")

    val flattenErrorList = errorList.flatten.mkString("\n")
    if (!flattenErrorList.isEmpty)
      BadRequest(flattenErrorList)
    else {
      try {
        val user: UserModel = UserModel(username, email, getSha512(password), firstName, lastName)
        val userId = Await.result(UserDao.add(user), Duration.Inf)
        repository.registerUser(username)
        val token = createToken(userId)
        loginResult(token, Some(user))
      } catch {
        case _: Throwable => BadRequest("Duplicate user name or email")
      }
    }
  }


  def getUser = AuthenticatedAction { implicit request =>
    val user = request.user
    loginResult(None, user)
  }

  def login = Action(parse.json) { implicit request =>
    val username = (request.body \ "username").asOpt[String].getOrElse("")
    Logger.debug("play.Play.isProd: " + play.Play.isProd)
    Logger.debug("play.Play.isDev: " + play.Play.isDev)
    if (Play.isProd && username.startsWith(SecurityControllerDefaults.GUEST_USER))
      BadRequest(s"Username is not acceptable, it is forbidden to start with ${SecurityControllerDefaults.GUEST_USER}")
    else {
      val password = (request.body \ "password").asOpt[String].getOrElse("")
      val userFuture = UserDao.getByUsername(username)
      val userOption = Await.result(userFuture, Duration.Inf).filter(_.shaPassword.deep == getSha512(password).deep)

      userOption match {
        case Some(user) =>
          loginResult(createToken(user.id), userOption)
        case None =>
          val result = "The username or password you entered don't match." + (if (Play.isDev) username + "-" + password else "")
          errorResult(result, Some(UNAUTHORIZED))
      }
    }
  }

  def loginGuest = Action { implicit request =>
    var username = SecurityControllerDefaults.GUEST_USER + guestCounter.incrementAndGet
    while (Await.result(UserDao.getByUsername(username), Duration.Inf).nonEmpty) {
      username = SecurityControllerDefaults.GUEST_USER + guestCounter.incrementAndGet
    }
    val user: UserModel = UserModel(username, username + "@demo.com", getSha512("password"), "Guest", "")
    val userId = Await.result(UserDao.add(user), Duration.Inf)
    repository.registerUser(username)
    val token = createToken(userId)
    if (Play.isDev)
      loginResult(token, Some(user))
    else
      loginResult(token, user = None)
  }

  def logout = AuthenticatedAction { request =>
    val username = request.username.get
    val user = request.user
    val authentication = request.authentication
    authentication match {
      case Some(authentication) =>
        invalidateToken(authentication.id)
        if (username.startsWith(SecurityControllerDefaults.GUEST_USER))
          repository.unregisterUser(username)
        Ok("Logout")
      case None =>
        NotFound("User not found")
    }
  }

  def loginResult(authToken: Option[String], user: Option[UserModel])(implicit request: RequestHeader): Result = {
    loginResult(authToken,
      user match {
        case Some(u) if !u.username.startsWith(SecurityControllerDefaults.GUEST_USER) => Some(u.username)
        case _ => None
      },
      user match {
        case Some(u) => Some(u.fullName)
        case _ => None
      })
  }

  def loginResult(authToken: Option[String], username: Option[String], fullName: Option[String])(implicit request: RequestHeader): Result = {
    render {
      // @formatter:off
      case Accepts.Xml() =>
        val xmlResult =
          <user>
            {if (authToken.isDefined)
            <authToken>{authToken}</authToken>
            }
            {if (username.isDefined)
            <username>{username.get}</username>
            }
            {if (fullName.isDefined)
            <fullName>{fullName.get}</fullName>
            }
          </user>
        Ok(xmlResult)
      // @formatter:on
      case Accepts.Json() =>
        val result = Json.obj(SecurityControllerDefaults.AUTH_TOKEN_COOKIE -> authToken) ++ (
          username match {
            case Some(u) => Json.obj("username" -> u)
            case _ => Json.obj()
          }) ++ (
          fullName match {
            case Some(u) => Json.obj("fullName" -> u)
            case _ => Json.obj()
          })
        Ok(result)
    }
    //      .withCookies(Cookie(AUTH_TOKEN, authToken, Some(Integer.MAX_VALUE)))
  }


  def errorResult(errorString: String, errorType: Option[Int] = None)(implicit request: RequestHeader): Result = {
    val status = errorType.getOrElse(BAD_REQUEST)
    render {
      // @formatter:off
      case Accepts.Xml() =>
        Status(status)(
          <error>
            <errorString>{errorString}</errorString>
            {if (errorType isDefined)
            <errorType>{errorType.get}</errorType>
            }
          </error>)
      // @formatter:on
      case Accepts.Json() =>
        val result = Json.obj("errorString" -> errorString) ++ (
          errorType match {
            case Some(_) => Json.obj("errorType" -> errorType)
            case _ => Json.obj()
          })
        Status(status)(result)
    }
  }

  def passwordRecoveryEmail(username: String) = Action { implicit request =>

    if (sendRecoveryEmail(username))
      Ok("Ok")
    else
      BadRequest(s"There is no user with this username($username), please check username and try again")
  }

  //authentication with token
  def passwordRecovery = AuthenticatedAction { implicit request =>
    val username = request.username.get

    Ok(views.html.password()).withCookies(Cookie(SecurityControllerDefaults.AUTH_TOKEN_COOKIE, request.getQueryString(SecurityControllerDefaults.QUERY_AUTH_TOKEN).get))
  }

  //authentication with token
  def updatePassword = AuthenticatedAction(parse.json) { implicit request =>
    val username = request.username.get
    val user = request.user.get
    val errorList = ListBuffer.empty[Option[String]]

    val password = (request.body \ "password").asOpt[String].getOrElse("")
    Logger.debug("password" + password)

    errorList += Validation.nonEmpty(password, "Password")

    val flattenErrorList = errorList.flatten.mkString("\n")

    if (!flattenErrorList.isEmpty)
      BadRequest(flattenErrorList)
    else {
      UserDao.updateShaPassword(user.id.get, getSha512(password))
      //      createToken(Some(user.id))
      Ok("OK").withCookies(Cookie(SecurityControllerDefaults.AUTH_TOKEN_COOKIE, "", Some(0)))
    }
  }

  def sendRecoveryEmail(username: String)(implicit request: Request[Any]) = {
    val userOption: Option[UserModel] = Await.result(UserDao.getByUsername(username), Duration.Inf)
    userOption match {
      case Some(user) =>
        val passwordRecovery = createToken(user.id)
        val link = routes.SecurityController.passwordRecovery.absoluteURL() + "?auth-token=" + passwordRecovery
        val userEmail = user.emailAddress
        val email = Email(
          subject = "GMQL password recovery"
          , from = "Polimi bioinformatics group <bioinformatics.polimi.it@gmail.com>"
          , to = Seq(userEmail)
          // adds attachment
          //      attachments = Seq(
          //        AttachmentFile("attachment.pdf", new File("/some/path/attachment.pdf")),
          // adds inline attachment from byte array
          //        AttachmentData("data.txt", "data".getBytes, "text/plain", Some("Simple data"), Some(EmailAttachment.INLINE)),
          // adds cid attachment
          //        AttachmentFile("image.jpg", new File("/some/path/image.jpg"), contentId = Some(cid))
          //      ),
          // sends text, HTML or both...
          , bodyText = Some(s"""Please open the recovery page:\n$link""")
          //        , bodyHtml = Some(s"""  """)
          , replyTo = Some("NO-REPLY@polimi.it")
        )
        mailerClient.send(email)
        true
      case None =>
        false
    }
  }


  def getSha512(value: String): Array[Byte] = MessageDigest.getInstance("SHA-512").digest(value.getBytes("UTF-8"))

  def createToken(id: Option[Long] = None): Some[String] = {
    val authToken = UUID.randomUUID().toString
    if (id.nonEmpty) {
      val future = AuthenticationDao.add(AuthenticationModel(id.get, None, authToken))
      Await.result(future, Duration.Inf)
    }
    Some(authToken)
  }


  def invalidateToken(tokenId: Option[Long] = None): Unit = {
    if (tokenId.nonEmpty) {
      val future = AuthenticationDao.delete(tokenId.get)
      Await.result(future, Duration.Inf)
    }
  }

}
