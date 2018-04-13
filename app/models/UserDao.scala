package models

import com.github.tototoshi.slick.H2JodaSupport._
import it.polimi.genomics.core.GDMSUserClass
import it.polimi.genomics.core.GDMSUserClass.GDMSUserClass
import org.joda.time.DateTime
import play.api.Play
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.H2Driver.api._
import slick.driver.JdbcProfile

import scala.concurrent.Future


object UserDao {
  implicit def gDMSUserClassMapper = MappedColumnType.base[GDMSUserClass, String](e => e.toString, s => GDMSUserClass.withNameOpt(s))




  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  val users = TableQuery[UserTableDef]

  def add(user: UserModel): Future[Option[Long]] = {
    //    val userId: FixedSqlAction[Long, NoStream, Write] =  (users returning users.map(_.id)) += user
    dbConfig.db.run((users returning users.map(_.id)) += user)
  }

  def delete(id: Long): Future[Int] = dbConfig.db.run(users.filter(_.id === id).delete)


  def disable(updatedUser: UserModel): Future[Int] = dbConfig.db.run(users.filter(_.id === updatedUser.id).map(user => user.deleted).update(true))


  def get(id: Long): Future[Option[UserModel]] = dbConfig.db.run(users.filter(_.id === id).result.headOption)


  def listAll: Future[Seq[UserModel]] = dbConfig.db.run(users.result)

  def listActive:  Future[Seq[UserModel]] = dbConfig.db.run(users.filter(!_.deleted).result)

  def count: Future[Int] = dbConfig.db.run(users.filter(_.userType === GDMSUserClass.ADMIN).length.result)


  def getByUsername(username: String): Future[Option[UserModel]] = dbConfig.db.run(users.filter(_.username === username).result.headOption)

  def updateShaPassword(id: Long, shaPassword: Array[Byte]) = dbConfig.db.run(users.filter(_.id === id).map(_.shaPassword).update(shaPassword))

  def updateType(username: String, newType: GDMSUserClass) = dbConfig.db.run( users.filter(_.username === username).map(_.userType).update(newType))

}

case class UserModel(username: String,
                     userType: GDMSUserClass,
                     emailAddress: String,
                     shaPassword: Array[Byte],
                     firstName: String,
                     lastName: String,
                     creationDate: DateTime = DateTime.now(),
                     lastUsedDate: DateTime = DateTime.now(),
                     deleted: Boolean = false,
                     id: Option[Long] = None) {
  def fullName = firstName + " " + lastName


}

//@formatter:off
class UserTableDef(tag: Tag) extends Table[UserModel](tag, "user") {
  import UserDao.gDMSUserClassMapper

  def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)

  def username = column[String]("username")

  def userType = column[GDMSUserClass]("user_type")

  def emailAddress = column[String]("email_address")

  def shaPassword = column[Array[Byte]]("sha_password")

  def firstName = column[String]("first_name")

  def lastName = column[String]("last_name")

  def creationDate = column[DateTime]("creation_date")

  def lastUsedDate = column[DateTime]("last_used_date")

  def deleted = column[Boolean]("deleted")

  override def * = (username, userType, emailAddress, shaPassword, firstName, lastName, creationDate, lastUsedDate, deleted, id) <> (UserModel.tupled, UserModel.unapply)


}

//@formatter:on



