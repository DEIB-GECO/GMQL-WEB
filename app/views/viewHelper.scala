package views

import play.api.mvc.Call
import play.twirl.api.Html


/**
  * Created by canakoglu on 4/18/16.
  */
object ViewHelper {
  def reformat(path: Call): Html = reformat(path.url)

  def reformat(path: String) = Html(
    path.replace("%7B", "{")
      .replace("%7D", "}")
      .replace("/", "/<wbr>")
    //      .replace(".",".<wbr>")
  )

  def formatList(list: List[String]) = Html(
    list.map(x => "\"" + x + "\"").mkString("[", ",", "]")
  )


}
