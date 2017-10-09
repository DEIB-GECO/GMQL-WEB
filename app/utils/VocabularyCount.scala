package utils

import java.io._

import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.collection.immutable.HashMap
import scala.concurrent.Future

/**
  * Created by canakoglu on 12/19/16.
  */
class VocabularyCount {
  var vocabularyMap: Map[(String, String), Int] = new HashMap

  //key value count
  def addVocabulary(stream: InputStream): InputStream = {
    //piped into the output. The cache size is small in order to send quickly
    val pis = new PipedInputStream(1024)
    val pos = new PipedOutputStream(pis)

    //getting the vocabulary counts.
    val pis2 = new PipedInputStream(1024*1024)
    val pos2 = new PipedOutputStream(pis2)

    Future {
      Iterator
        .continually(stream.read)
        .takeWhile(-1 !=)
        .foreach { a =>
          pos.write(a)
          pos2.write(a)
        }
      //        val buffer = new Array[Byte](1024)
      //        Stream.continually(data.read(buffer))
      //          .takeWhile(_ != -1)
      //          .foreach(zos.write(buffer, 0, _))
      pos2.close
      pos.close
//      println("FUTURE1 closed")
    }
    Future {
      val br = new BufferedReader(new InputStreamReader(pis2))
      Iterator.continually(br.readLine()).takeWhile(null !=).foreach { temp =>
        val list = temp.split("\\t")
        if (list.size == 2) {
          val tuple = (list.head, list.last)
          val count = vocabularyMap.get(tuple).getOrElse(0) + 1
          vocabularyMap = vocabularyMap + (tuple -> count)
        }
        else
          Logger.warn(s"Cannot parse $temp -> $list")
      }
      Logger.debug(s"Map: $vocabularyMap")
//      println("FUTURE2 closed")
    }
    pis
  }

  /**
    * stream the vocabularyMap as stream
    * @return
    */
  def getStream = {
    val pis = new PipedInputStream()
    val pos = new BufferedWriter(new OutputStreamWriter(new PipedOutputStream(pis)))
    Future {
      var tempKey: String = null
      //order by lowercase key and lowercase value.
      for (((key, value), count) <- vocabularyMap.toList.sortBy(x => (x._1._1.toLowerCase,x._1._2.toLowerCase))) {
        if (tempKey != key){//print first appearance the key
          pos.append(key)
          pos.newLine()
          tempKey = key
        }
        //print tab then value then count
        pos.append(s"\t$value\t$count")
        pos.newLine()
      }
      pos.close
    }
    pis
  }

}
