/**
  * Created by canakoglu on 6/17/16.
  */





import it.polimi.genomics.repository.FSRepository.DFSRepository
import play.Play
import play.api._



object Global extends GlobalSettings {


  override def beforeStart(app: Application) {
    Logger.info("Global.beforeStart")
//    System.setProperty("javax.xml.bind.context.factory","org.eclipse.persistence.jaxb.JAXBContextFactory")
    Logger.info("Global System property set: javax.xml.bind.context.factory=org.eclipse.persistence.jaxb.JAXBContextFactory")
  }

  override def onStart(app: Application) {
    Logger.info("Application has started ")
    utils.GMQL_Globals.apply
  }

  override def onStop(app: Application) {
    Logger.info("Application shutdown...   ")
  }

}