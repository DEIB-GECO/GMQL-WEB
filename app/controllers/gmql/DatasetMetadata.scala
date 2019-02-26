package controllers.gmql


import java.util.concurrent.ConcurrentHashMap

import it.polimi.genomics.repository.GMQLExceptions.GMQLSampleNotFound
import it.polimi.genomics.repository.{GMQLRepository, Utilities}
import net.sf.ehcache.CacheManager
import play.api.Logger
import play.api.cache.Cache
import utils.GmqlGlobal

import scala.collection.JavaConversions._
import scala.collection.convert.decorateAsScala._
import scala.collection.{concurrent, mutable}
import scala.concurrent.duration._


/**
  * Created by canakoglu on 3/2/17.
  */


case class Meta(id: Int, key: String, value: String)

class DatasetMetadata(username: String, datasetName: String, fullMap: mutable.Map[String, String] = mutable.Map.empty) {
  Logger.debug(s"DatasetMetadata is loading: $username -> $datasetName")
  val repository: GMQLRepository = GmqlGlobal.repository
  val ut: Utilities = GmqlGlobal.ut

  val (n, nameToId, idToName) = {
    var user: String = username
    var dsName = datasetName
    // if public then user name is public and get the correct dataset name
    if (datasetName.startsWith("public.")) {
      user = "public"
      dsName = dsName.substring("public.".length)
    }
    val samples = DatasetUtils.getSamples(user, dsName)
    val temp = samples.map(sample => (sample.name, sample.id.toInt))
    val n = if (temp.isEmpty) 0 else temp.map(_._2).max + 1
    val nameToId = temp.toMap
    val idToName = Array.ofDim[String](n)
    temp.foreach(t => idToName(t._2) = t._1)
    //    val idToName = temp.map(t => (t._2, t._1)).toMap
    Logger.debug(s"(nameToId, idToName) done: $username->$datasetName")
    (n, nameToId, idToName)
  }

  //  val n: Int = idToName.keySet.max

  //  def ids: Set[Int] = Range(0,n)


  //KEY VALUE -> Meta
  //    fullFile.groupBy(_.key).map(x => (x._1, x._2.groupBy(_.value) /*.map(x => (x._1, x._2.map(_.id)))*/ ))


  //KEY VALUE -> Meta


  val (keyBased, idBased) = {
    //
    val fullFile = {
      //to remove replicates if exists
      val seen = mutable.HashSet[Meta]()
      repository.getMetaIterator(datasetName, username).map(_.split("\t")).flatMap({
        case Array(id: String, key: String, value: String) => Some(Meta(id.toInt, fullMap.getOrElseUpdate(key, key), fullMap.getOrElseUpdate(value, value)))
        case _ => None
      }).filter { x =>
        if (seen(x))
          false
        else {
          seen += x
          true
        }
      }.toList
    }

    //if there are duplication
    Logger.debug(s"pre keyBased: $username->$datasetName")

    val keyBased: Map[String, Map[String, List[Meta]]] = generateKeyBased(fullFile)
    Logger.debug(s"between keyBased and idBased: $username->$datasetName")

    val idBased: Map[Int, List[Meta]] = fullFile.groupBy(_.id).map(x => (x._1, x._2))
    Logger.debug(s"Dataset loaded: $username->$datasetName")

    (keyBased, idBased)
  }


  private def generateKeyBased(full: List[Meta]) = full.groupBy(_.key).map(x => (x._1, x._2.groupBy(_.value).map(x => (x._1, x._2))))

  def getSampleMetadata(sampleName: String): AttributeList = {
    val idOption = nameToId.get(sampleName)
    idOption match {
      case Some(id) =>
        val attributeList = idBased.getOrDefault(id, List.empty).map(meta => Attribute(meta.key, Some(Value(meta.value)))).sorted
        AttributeList(attributeList)
      case None => throw new GMQLSampleNotFound(s"Sample not found, datasetName->sampleName: $datasetName -> $sampleName")
    }
  }

  def getAllKeys: AttributeList = {
    //    val attributeList1: Seq[Attribute] = fullFile
    //      .map(_.key)
    //      .groupBy(identity)
    //      .mapValues(_.size)
    //      .map(valueCount => Attribute(valueCount._1, sampleCount = Some(valueCount._2)))
    //      .toSeq
    //      .sorted
    //    //      .distinct.sorted.map(Attribute(_))\
    Logger.debug(s"getAllKeys pre $username->$datasetName")

    val attributeList = keyBased.map { case (key: String, valueMap: Map[String, Seq[Meta]]) =>
      val valueCount = valueMap.size
      val sampleCount = valueMap.values.flatten.size
      Attribute(key, valueCount = Some(valueCount), sampleCount = Some(sampleCount))
    }.toSeq.sorted
    Logger.debug(s"getAllKeys post $username->$datasetName")
    AttributeList(attributeList)
  }

  def getAllValues(key: String): Attribute = {
    //    val values2: Seq[Value] = fullFile
    //      .filter(_.key == key)
    //      .map(_.value)
    //      .groupBy(identity)
    //      .mapValues(_.size)
    //      //number of samples
    //      .map(valueCount => Value(valueCount._1, count = Some(valueCount._2)))
    //      .toSeq
    //      .sorted
    Logger.debug(s"getAllValues pre $username->$datasetName")

    val values: Seq[Value] = keyBased
      .getOrDefault(key, Map.empty)
      .map(value => Value(value._1, count = Some(value._2.size)))
      .toSeq
      .sorted
    Logger.debug(s"getAllValues post $username->$datasetName")

    Attribute(key, values = Some(values))
  }

  def getFilteredDatasets(attributeList: AttributeList): Dataset = {
    val samples = getIds(attributeList)
    val sampleList: Seq[Sample] = samples.map(sampleId => Sample(sampleId.toString, idToName(sampleId))).toSeq.sorted
    Dataset(s"${datasetName}_temp_result", samples = Some(sampleList))
  }

  //  var temp: Option[MatrixResult] = None

  def getFilteredMatrix(attributeList: AttributeList): MatrixResult = {
    //    if(temp.isDefined)
    //      return temp.get


    Logger.debug(s"getFilteredMatrix: $username -> $datasetName")
    val sampleList = {
      val samples = getIds(attributeList)
      samples.map(sampleId => Sample(sampleId.toString, idToName(sampleId))).toSeq.sorted
    }
    val sortedSampleIds = sampleList.map(_.id)


    val filteredKeyBasedUnordered: Map[String, Map[String, List[Meta]]] = getFilteredKeyBased(attributeList)
    val sortedKeys = filteredKeyBasedUnordered.keys.toSeq.sortBy(_.toLowerCase)

    val matrix: Seq[Seq[String]] = sortedKeys.map { key =>
      val valueMap: Map[String, List[Meta]] = filteredKeyBasedUnordered.getOrElse(key, Map.empty)
      val idToValues: Map[Int, Seq[String]] = valueMap.values.flatten.groupBy(_.id).map { case (id: Int, metaIt: Iterable[Meta]) =>
        (id, metaIt.map(_.value).toSeq.distinct)
      }
      val cellValues: Seq[Seq[String]] = sortedSampleIds.map { id => idToValues.getOrElse(id.toInt, Seq.empty) }
      cellValues.map { in =>
        if (in.length > 1)
          in.mkString("[", ", ", "]")
        else if (in.length == 1)
          in.head
        else
          null
      }
    }
    val res = MatrixResult(sampleList, sortedKeys.map(Attribute(_)), matrix /*.transpose*/)
    //temp += attributeList -> res
    res
  }


  def getFilteredKeys(attributeListInput: AttributeList): AttributeList = {
    Logger.debug(s"getFilteredKeys pre $username->$datasetName")

    val filteredKeyBased = getFilteredKeyBased(attributeListInput)

    val attributeList = filteredKeyBased.map { case (key: String, valueMap: Map[String, Seq[Meta]]) =>
      val valueCount = valueMap.size
      val sampleCount = valueMap.values.flatten.size
      Attribute(key, valueCount = Some(valueCount), sampleCount = Some(sampleCount))
    }.toSeq.sorted
    Logger.debug(s"getFilteredKeys post $username->$datasetName")

    AttributeList(attributeList)
  }


  def getFilteredValues(attributeListInput: AttributeList, key: String): Attribute = {
    Logger.debug(s"getFilteredValues pre $username->$datasetName")

    val filteredKeyBased = getFilteredKeyBased(attributeListInput)

    val values: Seq[Value] = filteredKeyBased
      .getOrDefault(key, Map.empty)
      .map(value => Value(value._1, count = Some(value._2.size)))
      .toSeq
      .sorted
    Logger.debug(s"getFilteredValues post $username->$datasetName")

    Attribute(key, values = Some(values))
  }


  private def getFilteredKeyBased(attributeListInput: AttributeList) = {
    val samples = getIds(attributeListInput)
    val filtered = idBased.filterKeys(samples.contains).values.flatten.toList
    generateKeyBased(filtered)
  }


  /**
    * Get all the sample IDs that is searched with attribute list.
    *
    * @param attributeList
    * @return
    */
  private def getIds(attributeList: AttributeList): Set[Int] = {
    var samples = Range(0, n).toSet
    for (attribute: Attribute <- attributeList.attributes if samples.nonEmpty) {
      val valueMapOption = keyBased.get(attribute.key)
      valueMapOption match {
        case Some(valueMap) =>
          val inputValues = attribute.getAllValues
          val foundSamples = inputValues.flatMap(inputValue => valueMap.get(inputValue.text).map(_.map(_.id))).flatten.toSet
          samples = samples intersect foundSamples
        case None => samples.clear()
      }
    }
    samples
  }

  Logger.debug(s"DatasetMetadata is loaded: $username -> $datasetName")
}

object DatasetMetadata {

  import play.api.Play.current

  val synchronizedLoadingSet: concurrent.Map[String, Unit] = new ConcurrentHashMap[String, Unit]().asScala

  var fullMapPublic: concurrent.Map[String, String] = new java.util.concurrent.ConcurrentHashMap[String, String]().asScala
  //  val fullMapPublic: mutable.Map[String, String] = mutable.Map.empty[String, String]


  //TODO remove
  def showMemory(text: String) = {
    val r: Runtime = Runtime.getRuntime

    // to test memory usage
    r.gc
    System.gc()
    System.runFinalization()
    r.gc
    System.gc()
    System.runFinalization()
    Logger.debug(s"Memory usage - $text: " + (r.totalMemory - r.freeMemory) / 1024.0 / 1024.0)
  }


  // use for preloading dataset of the user, default is public
  def loadCache(username: String = "public") = {
    //    import play.api.libs.concurrent.Execution.Implicits.defaultContext
    //    import scala.concurrent.Future
    for (ds <- utils.GmqlGlobal.repository.listAllDSs(username)) {
      // in order to load all public dataset in pararllel run as a future execution
      //Future {

      showMemory(s"before DatasetMetadata call $username->${ds.position}")
      //TODO remove
      val startTime = System.nanoTime

      DatasetMetadata(username, ds.position)


      //TODO remove
      val endTime = System.nanoTime
      val timeElapsed = (endTime - startTime) / 1000000000.0
      Logger.debug(s"Loading time $username->${ds.position}: $timeElapsed")


      showMemory(s"after DatasetMetadata call $username->${ds.position}")


      //}
    }
    fullMapPublic = new java.util.concurrent.ConcurrentHashMap[String, String]().asScala
    showMemory(s"after fullMapPublic.clear()")
    showMemory(s"after fullMapPublic.clear()")
  }

  def clearCache() = {
    val singletonManager = CacheManager.create
    Logger.debug(singletonManager.getCacheNames.toList.toString)
    singletonManager.getCache("play").removeAll()
    singletonManager.clearAll()
    fullMapPublic.clear()
  }


  def apply(username: String, datasetName: String): DatasetMetadata = {
    val dsKey = s"$username->$datasetName"
    val resultOption = Cache.get(dsKey)
    if (resultOption.nonEmpty)
      resultOption.get.asInstanceOf[DatasetMetadata]
    else {
      if (synchronizedLoadingSet.containsKey(dsKey))
        throw new Exception("Loading")
      else {
        synchronizedLoadingSet.put(dsKey, Unit)
        val result =
          if (username == "public")
            Cache.getOrElse(s"$username->$datasetName")(new DatasetMetadata(username: String, datasetName: String, fullMapPublic))
          else
            Cache.getOrElse(s"$username->$datasetName", 10.minutes)(new DatasetMetadata(username: String, datasetName: String))
        synchronizedLoadingSet.remove(dsKey)
        result
      }
    }
  }
}

