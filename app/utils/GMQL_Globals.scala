package utils

import it.polimi.genomics.repository.FSRepository.DFSRepository
import it.polimi.genomics.repository.Utilities
import play.Play
import play.api._
/**
  * Created by abdulrahman on 13/02/2017.
  */
class GMQL_Globals {
  val repository = new DFSRepository();
  val ut = Utilities()

}

object GMQL_Globals{
  var instance:GMQL_Globals = null
  def apply(): GMQL_Globals = {
    if (instance == null) {

      //Set the configuration folder location and isntanciate configurations of the repo.
      it.polimi.genomics.repository.Utilities.confFolder = Play.application().getFile("conf/").getAbsolutePath
      it.polimi.genomics.repository.Utilities()
      //create an instance form GMQL Global
      instance = new GMQL_Globals()
    }
    instance
  }
}