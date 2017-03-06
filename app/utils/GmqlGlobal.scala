package utils

import java.io.File

import it.polimi.genomics.repository.FSRepository.DFSRepository
import it.polimi.genomics.repository.Utilities
import play.api.Play
import play.api.Play.current


object GmqlGlobal {
  Utilities.confFolder = Play.application.getFile("conf/gmql_conf/").getAbsolutePath
  val repository = new DFSRepository()
  val ut: Utilities = Utilities()
  //
//  val managerUtilities = it.polimi.genomics.manager.Utilities()
}
