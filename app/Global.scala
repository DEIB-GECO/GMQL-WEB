/**
  * Created by canakoglu on 6/17/16.
  */


import controllers.gmql._
import play.api._
import utils.GmqlGlobal.repository

import scala.util.Try


object Global extends GlobalSettings {


  override def beforeStart(app: Application) {
    Logger.info("Global.beforeStart")
    Try(repository.registerUser(SecurityControllerDefaults.PUBLIC_USER))
  }

  override def onStart(app: Application) {
    Logger.info("Application has started ")
    //load all public user metadata into cache.
    if (true) {
//      import scala.concurrent.ExecutionContext.Implicits.global
      import play.api.libs.concurrent.Execution.Implicits.defaultContext
      import scala.concurrent.Future
      Future {
        Thread.sleep(30000)
        Logger.info("Loading datasets ")
        DatasetMetadata.loadCache()
        Logger.info("Datasets loaded  ")

      }
    }
  }

  override def onStop(app: Application) {
    Logger.info("Application shutdown...   ")
  }

}