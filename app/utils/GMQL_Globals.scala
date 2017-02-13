package utils

import it.polimi.genomics.repository.FSRepository.DFSRepository
import play.Play

/**
  * Created by abdulrahman on 13/02/2017.
  */
class GMQL_Globals {
  val repository = new DFSRepository();
}

object GMQL_Globals{
  var instance:GMQL_Globals = null
  def apply: GMQL_Globals = {
    if (instance == null) {
      //create an instance form GMQL Global
      instance = new GMQL_Globals()

      //Set the configuration folder location and isntanciate configurations of the repo.
      it.polimi.genomics.repository.Utilities.setConfDir(Play.application().getFile("conf/").getAbsolutePath)
      it.polimi.genomics.repository.Utilities()
    }
    instance
  }
}
