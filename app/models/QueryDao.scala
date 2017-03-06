package models

import com.github.tototoshi.slick.H2JodaSupport._
import org.joda.time.DateTime
import play.api.Play
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.H2Driver.api._
import slick.driver.JdbcProfile

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by canakoglu on 3/1/17.
  */
object QueryDao {
  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  val queries = TableQuery[QueryTableDef]

  def add(query: QueryModel): Future[String] = {
    dbConfig.db.run(queries += query).map(res => "Query successfully added").recover {
      case ex: Exception => ex.getCause.getMessage
    }
  }

  def delete(id: Long): Future[Int] = dbConfig.db.run(queries.filter(_.id === id).delete)


  def get(id: Long): Future[Option[QueryModel]] = dbConfig.db.run(queries.filter(_.id === id).result.headOption)


  def listAll: Future[Seq[QueryModel]] = dbConfig.db.run(queries.result)

  def getUserQueries(userId: Long) = dbConfig.db.run(queries.filter(_.userId === userId).filter(!_.deleted).result)

  def getUserQuery(userId: Long, name: String): Future[Option[QueryModel]] = dbConfig.db.run(queries.filter(_.userId === userId).filter(_.name === name).filter(!_.deleted).result.headOption)

  def updateQueryText(id: Long, queryText: String) = dbConfig.db.run(queries.filter(_.id === id).map(_.text).update(queryText))


}


case class QueryModel(userId: Long,
                      name: String,
                      text: String,
                      creationDate: DateTime = DateTime.now(),
                      lastUsedDate: DateTime = DateTime.now(),
                      deleted: Boolean = false,
                      id: Option[Long] = None)

//@formatter:off
class QueryTableDef(tag: Tag) extends Table[QueryModel](tag, "query") {
  def id = column[Option[Long]]("id", O.PrimaryKey,O.AutoInc)
  def userId = column[Long]("user_id")
  def name = column[String]("name")
  def text = column[String]("text")
  def creationDate = column[DateTime]("creation_date")
  def lastUsedDate = column[DateTime]("last_used_date")
  def deleted = column[Boolean]("deleted")

  override def * = (userId, name, text, creationDate, lastUsedDate, deleted, id) <> (QueryModel.tupled, QueryModel.unapply)
  def user = foreignKey("SUP_FK", id, TableQuery[UserTableDef])(_.id)
}
//@formatter:on

