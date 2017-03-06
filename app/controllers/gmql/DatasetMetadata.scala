package controllers.gmql

import it.polimi.genomics.repository.GMQLExceptions.GMQLSampleNotFound
import it.polimi.genomics.repository.{GMQLRepository, Utilities}
import utils.GmqlGlobal

import scala.collection.JavaConversions._


/**
  * Created by canakoglu on 3/2/17.
  */


case class Meta(id: String, key: String, value: String)

class DatasetMetadata(username: String, datasetName: String) {
  val repository: GMQLRepository = GmqlGlobal.repository
  val ut: Utilities = GmqlGlobal.ut

  lazy val (nameToId, idToName) = {
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
    (nameToId, idToName)
  }

  lazy val fullFile = repository.getMeta(datasetName, username).split("\n").map(_.split("\t")).flatMap({
    case Array(id: String, key: String, value: String) => Some(Meta(id, key, value))
    case _ => None
  }).toSet //if there are duplication

  //KEY VALUE -> Meta
  lazy val keyBased: Map[String, Map[String, Set[Meta]]] = generateKeyBased(fullFile)
  //    fullFile.groupBy(_.key).map(x => (x._1, x._2.groupBy(_.value) /*.map(x => (x._1, x._2.map(_.id)))*/ ))

  private def generateKeyBased(full: Set[Meta]) = full.groupBy(_.key).map(x => (x._1, x._2.groupBy(_.value) /*.map(x => (x._1, x._2.map(_.id)))*/ ))

  //change last element to id(String)   key-> value -> id
  //  lazy val keyBased2: Map[String, Map[String, Set[String]]] = keyBased.map(t1 => (t1._1, t1._2.map(t2 => (t2._1, t2._2.map(_.id)))))

  //KEY VALUE -> Meta
  lazy val idBased: Map[String, Map[String, Set[Meta]]] = fullFile.groupBy(_.id).map(x => (x._1, x._2.groupBy(_.key) /*.map(x => (x._1, x._2.map(_.id)))*/ ))


  //change last element to id(String)   key-> value -> id
  //  lazy val idBased2: Map[String, Map[String, Set[String]]] = keyBased.map(t1 => (t1._1, t1._2.map(t2 => (t2._1, t2._2.map(_.value)))))


  // (key,value) - > Set(id)
  //  lazy val keyValueBased2: Map[(String, String), Set[String]] = fullFile.groupBy(x => (x.key, x.value)).map(x => (x._1, x._2.map(_.id).toSet))


  lazy val ids: Set[String] = idToName.keySet


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
    val attributeList = keyBased.map { case (key: String, valueMap: Map[String, Seq[Meta]]) =>
      val valueCount = valueMap.size
      val sampleCount = valueMap.values.flatten.size
      Attribute(key, valueCount = Some(valueCount), sampleCount = Some(sampleCount))
    }.toSeq.sorted

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
    val values: Seq[Value] = keyBased
      .getOrDefault(key, Map.empty)
      .map(value => Value(value._1, count = Some(value._2.size)))
      .toSeq
      .sorted

    Attribute(key, values = Some(values))
  }

  def getFilteredDatasets(attributeList: AttributeList): Dataset = {
    val samples = getIds(attributeList)
    val sampleList: Seq[Sample] = samples.map(sampleId => Sample(sampleId, idToName(sampleId))).toSeq.sorted
    Dataset(s"${datasetName}_temp_result", samples = Some(sampleList))
  }


  def getFilteredKeys(attributeListInput: AttributeList): AttributeList = {
    val filteredKeyBased= getFilteredKeyBased(attributeListInput)

    val attributeList = filteredKeyBased.map { case (key: String, valueMap: Map[String, Seq[Meta]]) =>
      val valueCount = valueMap.size
      val sampleCount = valueMap.values.flatten.size
      Attribute(key, valueCount = Some(valueCount), sampleCount = Some(sampleCount))
    }.toSeq.sorted

    AttributeList(attributeList)
  }




  def getFilteredValues(attributeListInput: AttributeList, key: String): Attribute = {
    val filteredKeyBased= getFilteredKeyBased(attributeListInput)

    val values: Seq[Value] = filteredKeyBased
      .getOrDefault(key, Map.empty)
      .map(value => Value(value._1, count = Some(value._2.size)))
      .toSeq
      .sorted

    Attribute(key, values = Some(values))
  }


  def getFilteredKeyBased(attributeListInput: AttributeList) ={
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
}

object DatasetMetadata {
  def apply(username: String, datasetName: String) = new DatasetMetadata(username: String, datasetName: String)
}
