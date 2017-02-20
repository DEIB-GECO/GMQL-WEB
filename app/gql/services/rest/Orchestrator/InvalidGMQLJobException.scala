package gql.services.rest.Orchestrator

/**
  * Created by abdulrahman on 14/02/2017.
  */
case class InvalidGMQLJobException(message:String) extends Exception(message){

}

case class NoJobsFoundException(message:String) extends Exception(message){

}

case class GMQLServiceException(message:String) extends Exception(message){

}
