package controllers

import javax.inject.Singleton

import play.api.mvc.{Action, Controller}
import play.api.routing.JavaScriptReverseRouter

@Singleton
class JavascriptRoute extends Controller {

  def javascriptRoutes = Action { implicit request =>
    Ok(
      JavaScriptReverseRouter("jsRoutes")(
        controllers.routes.javascript.SecurityController.login,
        controllers.routes.javascript.SecurityController.logout,
        controllers.routes.javascript.SecurityController.loginGuest,
        controllers.routes.javascript.SecurityController.getUser,
        controllers.routes.javascript.SecurityController.registerUser,
        controllers.routes.javascript.SecurityController.updatePassword,
        controllers.routes.javascript.SecurityController.passwordRecoveryEmail,
        //
        //
        controllers.routes.javascript.Application.sampleMetadata,
        //        controllers.routes.javascript.Application.gmql,
        //
        controllers.gmql.routes.javascript.DSManager.getDatasets,
        controllers.gmql.routes.javascript.DSManager.getSamples,
        controllers.gmql.routes.javascript.DSManager.dataSetSchema,
        //
        controllers.gmql.routes.javascript.DSManager.uploadSample,
        controllers.gmql.routes.javascript.DSManager.uploadSamplesFromUrls,
        controllers.gmql.routes.javascript.DSManager.deleteDataset,
        controllers.gmql.routes.javascript.DSManager.zip,
        //        controllers.gmql.routes.javascript.DSManager.zipFilePreparation,
        //        controllers.gmql.routes.javascript.DSManager.downloadFileZip,
        controllers.gmql.routes.javascript.DSManager.parseFiles,
        controllers.gmql.routes.javascript.DSManager.getUcscLink,
        controllers.gmql.routes.javascript.DSManager.getUcscList,
        //        controllers.gmql.routes.javascript.DSManager.getSampleFile,
        //
        //

        controllers.gmql.routes.javascript.QueryMan.runQuery,
        controllers.gmql.routes.javascript.QueryMan.compileQuery,
        controllers.gmql.routes.javascript.QueryMan.getJobs,
        controllers.gmql.routes.javascript.QueryMan.traceJob,
        controllers.gmql.routes.javascript.QueryMan.getLog,
        controllers.gmql.routes.javascript.QueryMan.stopJob,
        //
        //        controllers.gmql.routes.javascript.RepositoryBro.dataSetAttributeList,
        //        controllers.gmql.routes.javascript.RepositoryBro.dataSetMetaAttribute,
        //        controllers.gmql.routes.javascript.RepositoryBro.dataSetMetaAttributeValue,
        //        controllers.gmql.routes.javascript.RepositoryBro.browseId,
        //        controllers.gmql.routes.javascript.RepositoryBro.getFilteredSamples,
        //

        controllers.gmql.routes.javascript.MetadataBrowser.getSampleMetadata,
        controllers.gmql.routes.javascript.MetadataBrowser.getKeys,
        controllers.gmql.routes.javascript.MetadataBrowser.getValues,
        controllers.gmql.routes.javascript.MetadataBrowser.getFilteredDataset,
        controllers.gmql.routes.javascript.MetadataBrowser.getFilteredKeys,
        controllers.gmql.routes.javascript.MetadataBrowser.getFilteredValues,


        controllers.gmql.routes.javascript.QueryBrowser.getQueries,
        controllers.gmql.routes.javascript.QueryBrowser.getQuery,
        controllers.gmql.routes.javascript.QueryBrowser.saveQuery

      )
    )
  }
}