package controllers

import javax.inject.Singleton

import play.api.mvc.{Action, Controller}
import play.api.routing.JavaScriptReverseRouter

@Singleton
class JavascriptRoute extends Controller {

  def javascriptRoutes = Action { implicit request =>
    Ok(
      JavaScriptReverseRouter("jsRoutes")(
        controllers.routes.javascript.SecurityControllerScala.login,
        controllers.routes.javascript.SecurityControllerScala.logout,
        controllers.routes.javascript.SecurityControllerScala.loginGuest,
        controllers.routes.javascript.SecurityControllerScala.getUser,
        controllers.routes.javascript.SecurityControllerScala.registerUser,
        controllers.routes.javascript.SecurityControllerScala.updatePassword,
        controllers.routes.javascript.SecurityControllerScala.passwordRecoveryEmail,


        controllers.routes.javascript.Application.sampleMetadata,
        controllers.routes.javascript.Application.gmql,

        controllers.gmql.routes.javascript.DSManager.dataSetAll,
        controllers.gmql.routes.javascript.DSManager.dataSetSamples,
        controllers.gmql.routes.javascript.DSManager.uploadSample,
        controllers.gmql.routes.javascript.DSManager.uploadSamplesFromUrls,
        controllers.gmql.routes.javascript.DSManager.dataSetDeletion,
        controllers.gmql.routes.javascript.DSManager.zipFilePreparation,
        controllers.gmql.routes.javascript.DSManager.downloadFileZip,
        controllers.gmql.routes.javascript.DSManager.parseFiles,
        controllers.gmql.routes.javascript.DSManager.getUcscLink,
        controllers.gmql.routes.javascript.DSManager.getUcscList,
        controllers.gmql.routes.javascript.DSManager.getSampleFile,


        controllers.gmql.routes.javascript.QueryMan.saveQueryAs,
        controllers.gmql.routes.javascript.QueryMan.runQueryV2File,
        controllers.gmql.routes.javascript.QueryMan.compileQueryV2File,
        controllers.gmql.routes.javascript.QueryMan.traceJobV2,
        controllers.gmql.routes.javascript.QueryMan.getJobsV2,
        controllers.gmql.routes.javascript.QueryMan.getLog,
        controllers.gmql.routes.javascript.QueryMan.stopJob,

        controllers.gmql.routes.javascript.RepositoryBro.dataSetSchema,
        controllers.gmql.routes.javascript.RepositoryBro.dataSetMeta,
        controllers.gmql.routes.javascript.RepositoryBro.dataSetMetaAttribute,
        controllers.gmql.routes.javascript.RepositoryBro.dataSetMetaAttributeValue,
        controllers.gmql.routes.javascript.RepositoryBro.browseId,
        controllers.gmql.routes.javascript.RepositoryBro.getFilteredSamples,

        controllers.gmql.routes.javascript.RepositoryBro.getQueries,
        controllers.gmql.routes.javascript.RepositoryBro.getQuery

      )
    )
  }
}