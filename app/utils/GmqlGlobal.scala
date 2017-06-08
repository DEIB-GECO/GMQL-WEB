package utils

import it.polimi.genomics.repository.{GMQLRepository, Utilities}
import play.api.Play
import play.api.Play.current


object GmqlGlobal {
  Utilities.confFolder = Play.application.getFile("conf/gmql_conf/").getAbsolutePath
  val ut: Utilities = Utilities()
  val repository: GMQLRepository = ut.getRepository()
  //
//  val managerUtilities = it.polimi.genomics.manager.Utilities()
}
