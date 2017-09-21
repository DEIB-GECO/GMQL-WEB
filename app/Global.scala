/**
  * Created by canakoglu on 6/17/16.
  */


import controllers.gmql._
import play.api._
import utils.GmqlGlobal.repository


object Global extends GlobalSettings {


  override def beforeStart(app: Application) {
    Logger.info("Global.beforeStart")
    try {
      repository.registerUser(SecurityControllerDefaults.PUBLIC_USER)
    }
  }

  override def onStart(app: Application) {
    Logger.info("Application has started ")
    //load all public user metadata into cache.
    if (play.Play.isProd) {
      import scala.concurrent.ExecutionContext.Implicits.global
      import scala.concurrent.Future
      Future {
        Thread.sleep(30000)
        Logger.info("Loading datasets ")
        DatasetMetadata.loadCache()
      }
    }
  }

  override def onStop(app: Application) {
    Logger.info("Application shutdown...   ")
  }

}