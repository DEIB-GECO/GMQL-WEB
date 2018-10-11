package utils

import it.polimi.genomics.repository.federated.GF_Interface
import it.polimi.genomics.repository.{GMQLRepository, Utilities}
import play.api.Play
import play.api.Play.current


object GmqlGlobal {
  Utilities.confFolder = Play.application.getFile("conf/gmql_conf/").getAbsolutePath
  val ut: Utilities = Utilities()
  val repository: GMQLRepository = ut.getRepository()

  var federated_interface: Option[GF_Interface] =
    if( Utilities().GF_ENABLED ) {
      Some(GF_Interface.instance())
    } else {
      None
    }
  //
//  val managerUtilities = it.polimi.genomics.manager.Utilities()
}
