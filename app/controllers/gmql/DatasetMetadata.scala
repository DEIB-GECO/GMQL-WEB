package controllers.gmql

import it.polimi.genomics.repository.GMQLExceptions.GMQLSampleNotFound
import it.polimi.genomics.repository.{GMQLRepository, Utilities}
import net.sf.ehcache.CacheManager
import play.api.Logger
import play.api.cache.Cache
import utils.GmqlGlobal

import scala.collection.JavaConversions._
import scala.concurrent.duration._


/**
  * Created by canakoglu on 3/2/17.
  */


case class Meta(id: String, key: String, value: String)

class DatasetMetadata(username: String, datasetName: String) {
  Logger.debug(s"DatasetMetadata is loading: $username -> $datasetName")
  val repository: GMQLRepository = GmqlGlobal.repository
  val ut: Utilities = GmqlGlobal.ut

  val (nameToId, idToName) = {
    var user: String = username
    var dsName = datasetName
    // if public then user name is public and get the correct dataset name
    if (datasetName.startsWith("public.")) {
      user = "public"
      dsName = dsName.substring("public.".length)
    }
    val samples = DatasetUtils.getSamples(user, dsName)
    val temp = samples.map(sample => (sample.name, sample.id))
    val nameToId = temp.toMap
    val idToName = temp.map(t => (t._2, t._1)).toMap
    Logger.debug(s"(nameToId, idToName) done: $username->$datasetName")
    (nameToId, idToName)
  }
  val ids: Set[String] = idToName.keySet


  //KEY VALUE -> Meta
  //    fullFile.groupBy(_.key).map(x => (x._1, x._2.groupBy(_.value) /*.map(x => (x._1, x._2.map(_.id)))*/ ))


  //KEY VALUE -> Meta


  val (keyBased, idBased) = {
    val fullFile = repository.getMeta(datasetName, username).split("\n").map(_.split("\t")).flatMap({
      case Array(id: String, key: String, value: String) => Some(Meta(id, key, value))
      case _ => None
    }).toSet
    //if there are duplication
    Logger.debug(s"pre keyBased: $username->$datasetName")
    val keyBased: Map[String, Map[String, Set[Meta]]] = generateKeyBased(fullFile)
    Logger.debug(s"between keyBased and idBased: $username->$datasetName")
    val idBased: Map[String, Map[String, Set[Meta]]] = fullFile.groupBy(_.id).map(x => (x._1, x._2.groupBy(_.key) /*.map(x => (x._1, x._2.map(_.id)))*/ ))
    Logger.debug(s"Dataset loaded: $username->$datasetName")
    (keyBased, idBased)
  }


  private def generateKeyBased(full: Set[Meta]) = full.groupBy(_.key).map(x => (x._1, x._2.groupBy(_.value) /*.map(x => (x._1, x._2.map(_.id)))*/ ))

  def getSampleMetadata(sampleName: String): AttributeList = {
    val idOption = nameToId.get(sampleName)
    idOption match {
      case Some(id) =>
        val attributeList = idBased.getOrDefault(id, Map.empty).values.flatten.map(meta => Attribute(meta.key, Some(Value(meta.value)))).toSeq.sorted
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
    val sampleList: Seq[Sample] = samples.map(sampleId => Sample(sampleId, idToName(sampleId))).toSeq.sorted
    Dataset(s"${datasetName}_temp_result", samples = Some(sampleList))
  }

  //  var temp: Option[MatrixResult] = None

  def getFilteredMatrix(attributeList: AttributeList): MatrixResult = {
    //    if(temp.isDefined)
    //      return temp.get


    Logger.debug(s"getFilteredMatrix: $username -> $datasetName")
    val sampleList = {
      val samples: Set[String] = getIds(attributeList)
      samples.map(sampleId => Sample(sampleId, idToName(sampleId))).toSeq.sorted
    }
    val sortedSampleIds = sampleList.map(_.id)


    val filteredKeyBasedUnordered: Map[String, Map[String, Set[Meta]]] = getFilteredKeyBased(attributeList)
    val sortedKeys = filteredKeyBasedUnordered.keys.toSeq.sortBy(_.toLowerCase)

    val matrix: Seq[Seq[String]] = sortedKeys.map { key =>
      val valueMap: Map[String, Set[Meta]] = filteredKeyBasedUnordered.get(key).getOrElse(Map.empty)
      val idToValues: Map[String, Seq[String]] = valueMap.values.flatten.groupBy(_.id).map { case (id: String, metaIt: Iterable[Meta]) =>
        (id, metaIt.map(_.value).toSeq.distinct)
      }
      val cellValues: Seq[Seq[String]] = sortedSampleIds.map { id => idToValues.get(id).getOrElse(Seq.empty) }
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
    val filtered: Set[Meta] = idBased.filterKeys(samples.contains).values.flatMap(_.values.flatten).toSet
    generateKeyBased(filtered)
  }


  /**
    * Get all the sample IDs that is searched with attribute list.
    *
    * @param attributeList
    * @return
    */
  private def getIds(attributeList: AttributeList): Set[String] = {
    var samples = ids
    for (attribute: Attribute <- attributeList.attributes if samples.nonEmpty) {
      val valueMapOption = keyBased.get(attribute.key)
      valueMapOption match {
        case Some(valueMap) =>
          val inputValues = attribute.getAllValues
          val foundSamples: Set[String] = inputValues.flatMap(inputValue => valueMap.get(inputValue.text).map(_.map(_.id))).flatten.toSet
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

  val synchronizedLoadingSet = new java.util.concurrent.ConcurrentHashMap[String, Unit]()

  // use for preloading dataset of the user, default is public
  def loadCache(username: String = "public") = {
    //    import play.api.libs.concurrent.Execution.Implicits.defaultContext
    //    import scala.concurrent.Future
    for (ds <- utils.GmqlGlobal.repository.listAllDSs(username)) {
      // in order to load all public dataset in pararllel run as a future execution
      //Future {
      DatasetMetadata(username, ds.position)
      //}
    }
  }

  def clearCache() = {
    val singletonManager = CacheManager.create
    Logger.debug(singletonManager.getCacheNames.toList.toString)
    singletonManager.getCache("play").removeAll()
    singletonManager.clearAll()
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
            Cache.getOrElse(s"$username->$datasetName")(new DatasetMetadata(username: String, datasetName: String))
          else
            Cache.getOrElse(s"$username->$datasetName", 10.minutes)(new DatasetMetadata(username: String, datasetName: String))
        synchronizedLoadingSet.remove(dsKey)
        result
      }
    }
  }
}

