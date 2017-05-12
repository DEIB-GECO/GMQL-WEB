/**
  * Created by canakoglu on 6/17/16.
  */


import controllers.gmql.DatasetMetadata
import play.api._

object Global extends GlobalSettings {


  override def beforeStart(app: Application) {
    Logger.info("Global.beforeStart")
  }

  override def onStart(app: Application) {
    Logger.info("Application has started ")
    //load all public user metadata into cache.
//    if (play.Play.isProd) {
      import scala.concurrent.ExecutionContext.Implicits.global
      import scala.concurrent.Future
//      Future {
        DatasetMetadata.loadCache()
//      }
//    }
  }

  override def onStop(app: Application) {
    Logger.info("Application shutdown...   ")
  }

}