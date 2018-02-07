package models

import com.github.tototoshi.slick.H2JodaSupport._
import org.joda.time.DateTime
import play.api.Play
import play.api.Play.current
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.H2Driver.api._
import slick.driver.JdbcProfile

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future

/**
  * Created by canakoglu on 3/1/17.
  */
object AuthenticationDao {
  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  val authentications = TableQuery[AuthenticationTableDef]

  def add(authentication: AuthenticationModel): Future[String] = {
    dbConfig.db.run(authentications += authentication).map(res => "Authentitation successfully added").recover {
      case ex: Exception => ex.getCause.getMessage
    }
  }

  def delete(id: Long): Future[Int] = dbConfig.db.run(authentications.filter(_.id === id).delete)

  def deleteAll(userId: Long): Future[Int] = dbConfig.db.run(authentications.filter(_.userId === userId).delete)


  def get(id: Long): Future[Option[AuthenticationModel]] = dbConfig.db.run(authentications.filter(_.id === id).result.headOption)


  def listAll: Future[Seq[AuthenticationModel]] = dbConfig.db.run(authentications.result)


  def getByToken(authToken: String): Future[Option[(UserModel, AuthenticationModel)]] = {
    if (Play.isDev && authToken.equals("test-best-token"))
      dbConfig.db.run(UserDao.users.filter(_.username === "canakoglu").result.headOption).map(_.map(user => (user, AuthenticationModel(user.id.get, None, "test-best-token"))))
    else
      dbConfig.db.run((for {
        u <- UserDao.users
        a <- authentications if a.userId === u.id
      } yield (u, a)).filter(_._2.authToken === authToken).result.headOption)
  }
}


case class AuthenticationModel(userId: Long,
                               authType: Option[String],
                               authToken: String,
                               creationDate: DateTime = DateTime.now(),
                               lastUsedDate: DateTime = DateTime.now(),
                               id: Option[Long] = None)

//@formatter:off
class AuthenticationTableDef(tag: Tag) extends Table[AuthenticationModel](tag, "authentication") {
  def id = column[Option[Long]]("id", O.PrimaryKey,O.AutoInc)
  def userId = column[Long]("user_id")
  def authType = column[Option[String]]("auth_type")
  def authToken = column[String]("auth_token")
  def creationDate = column[DateTime]("creation_date")
  def lastUsedDate = column[DateTime]("last_used_date")

  override def * = (userId, authType, authToken, creationDate, lastUsedDate, id) <> (AuthenticationModel.tupled, AuthenticationModel.unapply)
  def user = foreignKey("SUP_FK", id, TableQuery[UserTableDef])(_.id)
}
//@formatter:on

