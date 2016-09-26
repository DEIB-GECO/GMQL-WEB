package controllers.gmql

import javax.inject.Singleton

import gql.services.rest.MetadataBrowserBasic
import play.api.mvc.Controller
import wrappers.authanticate.AuthenticatedAction

/**
  * Created by canakoglu on 4/11/16.
  */
@Singleton
class MetadataBrowser extends Controller {

  /**
    * Browses a metadata file to get the set of unique attributes it contains.
    *
    * @return
    */
  def browseResourceFile(fileKey: String) = AuthenticatedAction { request =>
    val response = new MetadataBrowserBasic().browseResourceFile(fileKey)
    val resAsString = ResultUtils.unMarshall(response, false)
    Ok(resAsString).as("text/xml")
  }


  def browseAttribute(fileKey: String, attribute:String ) = AuthenticatedAction { request =>
    val response = new MetadataBrowserBasic().browseAttribute(fileKey, attribute)
    val resAsString = ResultUtils.unMarshall(response, false)
    Ok(resAsString).as("text/xml")
  }

  def browseAttributeValue(fileKey: String, attribute:String ,value:String ) = AuthenticatedAction { request =>
    val response = new MetadataBrowserBasic().browseAttributeValue(fileKey, attribute, value)
    val resAsString = ResultUtils.unMarshall(response, false)
    Ok(resAsString).as("text/xml")
  }



  def browseId(fileKey: String, id:String ) = AuthenticatedAction { request =>
    val response = new MetadataBrowserBasic().browseId(fileKey, id)
    val resAsString = ResultUtils.unMarshall(response, false)
    Ok(resAsString).as("text/xml")
  }

  def getAllExperiments(fileKey: String, attribute:String ,value:String ) = AuthenticatedAction { request =>
    val response = new MetadataBrowserBasic().getAllExperiments(fileKey, attribute, value)
    val resAsString = ResultUtils.unMarshall(response, false)
    Ok(resAsString).as("text/xml")
  }


  //TODO it is empty in the other file
  def filterExperiments(fileKey: String, attribute:String ,values: List[String] ) = TODO


}
