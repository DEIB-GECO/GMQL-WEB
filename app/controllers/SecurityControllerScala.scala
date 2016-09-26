package controllers

import java.util.concurrent.atomic.AtomicInteger
import javax.inject.{Inject, Singleton}

import gql.services.rest.DataSetsManager
import models.User
import play.api.Logger
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation._
import play.api.libs.json._
import play.api.libs.mailer._
import play.api.mvc._
import wrappers.authanticate.AuthenticatedAction

import scala.collection.mutable.ListBuffer
import scala.util.Try


object SecurityControllerDefaults {

  val AUTH_TOKEN_HEADER: String = "X-Auth-Token"
  val AUTH_TOKEN_COOKIE: String = "authToken"
  val QUERY_AUTH_TOKEN: String = "auth-token"
  val GUEST_USER: String = "guest"
  val PUBLIC_USER: String = "public"
}

/**
  * Created by canakoglu on 6/13/16.
  */
@Singleton
class SecurityControllerScala @Inject()(mailerClient: MailerClient) extends Controller {


  private val guestCounter: AtomicInteger = new AtomicInteger

  case class UserData(username: String, password: String, email:String, firstName:String, lastName:String)

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
    if(!flattenErrorList.isEmpty)
      BadRequest(flattenErrorList)
    else{
      try{
      val user: User = new User(username, email, password, firstName, lastName)
      user.save
      new DataSetsManager().registerUser(username)
      loginResult(Some(user.createToken), Some(user))
      }catch {
        case _ => BadRequest("Duplicate user name or email")
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
    if (play.Play.isProd && username.startsWith(SecurityControllerDefaults.GUEST_USER))
      BadRequest(s"Username is not acceptable, it is forbidden to start with ${SecurityControllerDefaults.GUEST_USER}")
    else {
      val password = (request.body \ "password").asOpt[String].getOrElse("")
      val userOption = Option(User.findByUsernameAndPassword(username, password));
      userOption match {
        case Some(user) =>
          loginResult(Some(user.createToken()), userOption)
        case None =>
          val result = "The username or password you entered don't match." + (if (play.Play.isDev) username + "-" + password else "")
          errorResult(result, Some(UNAUTHORIZED))
      }
    }
  }

  def loginGuest = Action { implicit request =>
    var username = SecurityControllerDefaults.GUEST_USER + guestCounter.incrementAndGet
    while (User.existsByUsername(username)) {
      username = SecurityControllerDefaults.GUEST_USER + guestCounter.incrementAndGet
    }
    val user: User = new User(username, username + "@demo.com", "password", "Guest", "")
    user.save
    new DataSetsManager().registerUser(username)
    if (play.Play.isDev())
      loginResult(Some(user.createToken), Some(user))
    else
      loginResult(Some(user.createToken), user = None)

  }

  def logout = AuthenticatedAction { request =>
    val user = request.user.get
    if (user != null) {
      user.deleteAuthToken
      if (user.getUsername.startsWith("guest"))
        new DataSetsManager().unRegisterUser(user.getUsername)
      Ok("Logout")
    }
    else
      NotFound("User not found")
  }

  def loginResult(authToken: Option[String], user: Option[User])(implicit request: RequestHeader): Result = {
    loginResult(authToken,
      user match {
        case Some(u) if u.getUsername.startsWith(SecurityControllerDefaults.GUEST_USER) => Some(u.getUsername)
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
            {if (authToken isDefined)
            <authToken>{authToken}</authToken>
            }
            {if (username isDefined)
            <username>{username.get}</username>
            }
            {if (username isDefined)
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

    if(sendRecoveryEmail(username))
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

    if(!flattenErrorList.isEmpty)
      BadRequest(flattenErrorList)
    else{
        user.setPassword(password)
        user.createToken()
        Ok("OK").withCookies(Cookie(SecurityControllerDefaults.AUTH_TOKEN_COOKIE, "",Some(0)))

    }


  }

  def sendRecoveryEmail(username: String)(implicit request:Request[Any]) = {
    val user = User.findByUsername(username)
    if (user != null) {
      val passwordRecovery = user.createToken()
      val link = routes.SecurityControllerScala.passwordRecovery.absoluteURL() + "?auth-token=" + passwordRecovery
      val userEmail = user.getEmailAddress
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
    }
    else
      false
  }

}
