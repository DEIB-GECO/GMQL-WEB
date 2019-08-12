package controllers.gmql

import javax.inject.Singleton
import controllers.gmql.ResultUtils.NA
import io.swagger.annotations._
import it.polimi.genomics.core.GDMSUserClass
import it.polimi.genomics.repository.Utilities
import models.UserDao
import play.api.Logger
import play.api.libs.json._
import play.api.mvc.{Action, Controller, Results}
import wrappers.authanticate.AuthenticatedAction

import scala.concurrent.duration.Duration
import scala.concurrent.Await


@Singleton
@Api(value = SwaggerUtils.swaggerSystemAdmin, produces = "application/json, application/xml")
class AdminManager extends Controller {

  val allowedUserTypes =  GDMSUserClass.values.filter(x => {
    !x.equals(GDMSUserClass.ALL) && !x.equals(GDMSUserClass.PUBLIC) && !x.equals(GDMSUserClass.GUEST)
  }).map(_.toString)

  implicit val userFormat = Json.writes[User]
  implicit val usersFormat = Json.writes[Users]
  implicit val userTypesFormat = Json.writes[UserTypes]

  def adminPage = Action { implicit request =>
    Ok(views.html.admin_main())
  }

  /**
    * Return info about the instance (e.g. email of the administrator)
    *
    * @return list of infos
    */
  @ApiOperation(value = "Get instance info",
    notes = "E.g. adminEmail")
  @ApiImplicitParams(Array(new ApiImplicitParam(name = "X-AUTH-TOKEN", dataType = "string", paramType = "header", required = false)))
  @ApiResponses(value = Array())
  def getInfo = Action { implicit request =>

    var info = Map[String, String]()

    if(Utilities().ADMIN_EMAIL.isDefined)
      info = info + ("adminEmail" -> Utilities().ADMIN_EMAIL.get)



      render {
        case Accepts.Xml() => Ok(<info>{info.keys.map(k => <item name={k}>{info(k)}</item>)}</info>)
        case Accepts.Json() => Ok(Json.toJson(info))
        case _ => NA
      }
  }

  /**
    * Return the list of all users registered to the system with some info (email, user_category, subscriprion timestamp)
    *
    * @return list of users and their info
    */
  @ApiOperation(value = "Get the list of all users",
    notes = "Get the list of users. Allowed only to admin users.",
    response = classOf[Users])
  @ApiImplicitParams(Array(new ApiImplicitParam(name = "X-AUTH-TOKEN", dataType = "string", paramType = "header", required = true)))
  @ApiResponses(value = Array(
    new ApiResponse(code = 401, message = "User is not authenticated"),
    new ApiResponse(code = 403, message = "User is not authorized")))
  def getUsers = AuthenticatedAction { implicit request =>


    if( !request.user.get.userType.equals(GDMSUserClass.ADMIN) ) {

      val statusFunction =  new Results.Status(403)
      render {
        case Accepts.Xml() => statusFunction(<error>Allowed only to administrators.</error>)
        case Accepts.Json() =>  statusFunction("{'error': 'Allowed only to administrators.'}")
        case _ => statusFunction(s"Error: Allowed only to administrators.")
      }

    } else {

      lazy val users: Users = {
        val dbusers = Await.result(UserDao.listActive, Duration.Inf)
        val userlist = for (user <- dbusers) yield User(user.username, user.firstName, user.lastName, user.emailAddress, user.userType.toString)
        Users(userlist)
      }

      render {
        case Accepts.Xml() => Ok(scala.xml.Utility.trim(users.getXml))
        case Accepts.Json() => Ok(Json.toJson(users))
        case _ => NA
      }
    }
  }

  /**
    * Return the lists of user types
    *
    * @return list of users and their info
    */
  @ApiOperation(value = "Get the list of user types",
    notes = "Get the list of user types. Allowed only to admin users.",
    response = classOf[UserTypes])
  @ApiImplicitParams(Array(new ApiImplicitParam(name = "X-AUTH-TOKEN", dataType = "string", paramType = "header", required = true)))
  @ApiResponses(value = Array(
    new ApiResponse(code = 401, message = "User is not authenticated"),
    new ApiResponse(code = 403, message = "User is not authorized")))
  def getUserTypes = AuthenticatedAction { implicit request =>

    if( !request.user.get.userType.equals(GDMSUserClass.ADMIN) ) {

      val statusFunction =  new Results.Status(403)
      render {
        case Accepts.Xml() => statusFunction(<error>Allowed only to administrators.</error>)
        case Accepts.Json() =>  statusFunction("{'error': 'Allowed only to administrators.'}")
        case _ => statusFunction(s"Error: Allowed only to administrators.")
      }

    } else {
      lazy val userTypes: UserTypes = {
        val userTypes = allowedUserTypes.toSeq
        UserTypes(userTypes)
      }

      render {
        case Accepts.Xml() => Ok(scala.xml.Utility.trim(userTypes.getXml))
        case Accepts.Json() => Ok(Json.toJson(userTypes))
        case _ => NA
      }
    }
  }

  @ApiOperation(value = "Update user type",
    notes = "Update the user type (category). Allowed only to admin users.")
  @ApiResponses(value = Array(
    new ApiResponse(code = 401, message = "User is not authenticated"),
    new ApiResponse(code = 403, message = "User is not authorized"),
    new ApiResponse(code = 404, message = "Dataset is not found for the user")
  ))
  @ApiImplicitParams(Array(
    new ApiImplicitParam( name = "type", paramType = "body", dataType = "string",
      allowableValues = "BASIC, PRO, ADMIN",
      examples = new Example(Array(new ExampleProperty(value = """{"type": "PRO"}"""))),
      required = true),
    new ApiImplicitParam(name = "X-AUTH-TOKEN", dataType = "string", paramType = "header", required = true)
  ))
  def updateType(username:String) = AuthenticatedAction(parse.json) { implicit request =>

    if( !request.user.get.userType.equals(GDMSUserClass.ADMIN) ) {

      val statusFunction =  new Results.Status(403)

      render {
        case Accepts.Xml() => statusFunction(<error>Allowed only to administrators.</error>)
        case Accepts.Json() =>  statusFunction("{'error': 'Allowed only to administrators.'}")
        case _ => statusFunction(s"Error: Allowed only to administrators.")
      }

    } else {
      val typeString =  (request.body \ "type").asOpt[String].getOrElse("")
      Logger.info(typeString)

      UserDao.updateType(username, GDMSUserClass.withName(typeString) )
      Ok("success")
    }

  }

}

case class User( username: String,
                 firstName: String,
                 lastName: String,
                 emailAddress: String,
                 userType: String)  {

  @ApiModelProperty(hidden = true)
  def getXml =
    <user>
      <username>{username}</username>
      <first_name>{firstName}</first_name>
      <last_name>{lastName}</last_name>
      <email_address>{emailAddress}</email_address>
      <user_type>{userType}</user_type>
    </user>
}


case class Users(users: Seq[User]) {

  @ApiModelProperty(hidden = true)
  def getXml = <users>{users.map(_.getXml)}</users>

}


case class UserTypes(userTypes: Seq[String]) {
  @ApiModelProperty(hidden = true)
  def getXml = <userTypes>{userTypes.map(x => <userType>x.toString</userType>)}</userTypes>
}





