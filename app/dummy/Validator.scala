import javax.xml.transform.stream.StreamSource
import javax.xml.validation.Schema
import javax.xml.validation.SchemaFactory
import javax.xml.validation.{Validator => JValidator}
import org.xml.sax.SAXException

object Validator {
  def main(args: Array[String]) {
    require(args.size >= 2, "Params: xmlFile, xsdFile")
    val result =
      if (validate(args(0), args(1)))
        "Validates!"
      else
        "Not valid."
    println(result)
  }

  val dir = System.getProperty("user.dir") + "/app/dummy/"

  def validate(xmlFile: String, xsdFile: String): Boolean = {
    println(dir)
    try {
      val schemaLang = "http://www.w3.org/2001/XMLSchema"
      val factory = SchemaFactory.newInstance(schemaLang)
      val schema = factory.newSchema(new StreamSource(dir + xsdFile))
      val validator = schema.newValidator()
      validator.validate(new StreamSource(dir + xmlFile))
    } catch {
      case ex: SAXException =>
        println("--------------------------------------------------------------------")
        println("asd:" + ex.getMessage())
        return false
      case ex: Exception => ex.printStackTrace()
    }
    true
  }
}