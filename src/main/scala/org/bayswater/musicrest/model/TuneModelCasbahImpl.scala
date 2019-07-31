/*
 * Copyright (C) 2011-2013 org.bayswater
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bayswater.musicrest.model

import spray.util.LoggingContext
import com.mongodb.casbah.query.Imports._
import com.mongodb.casbah._
import com.mongodb.casbah.WriteConcern.Safe
import com.mongodb.DBObject
import scalaz.Validation
import scalaz.syntax.validation._
import scala.collection.JavaConversions._
import org.bayswater.musicrest.abc.{Abc, AbcMongo, TuneRef}
import org.bayswater.musicrest.abc.SupportedGenres

class TuneModelCasbahImpl(val mongoClient: MongoClient, val dbname: String) extends TuneModel with MongoTransaction {

  val log = implicitly[LoggingContext]
  val mongoDB = mongoClient(dbname)



  // set the write concern to Safe
  val writeConcern = {
    mongoDB.setWriteConcern(WriteConcern.Safe)
    mongoDB.getWriteConcern
  }

  def testConnection(): Boolean = try {
    count("irish", Map.empty[String, String])
    true
  }
  catch {
    case t: Throwable => log.error("Couldn't connect to users collection on database " + dbname)
    false
  }

  def insert(genre: String, abc:Abc): Validation[String, String] = withMongoPseudoTransction (mongoDB) {
    val mongoCollection = mongoClient(dbname)(genre)
    val builder = MongoDBObject.newBuilder[String, String]
    builder += "T" -> abc.toAbcMongo.mongoTitles
    builder ++= abc.toAbcMongo.kvs
    builder += TuneModel.tuneKey -> abc.id
    mongoCollection += builder.result
    "Tune " + abc.name + " inserted into " + genre
  }

  /** replace an existing tune (i.e. one with an existing tune _id) */
  def replace(id: ObjectId, genre: String, abc:Abc): Validation[String, String] = withMongoPseudoTransction (mongoDB) {
    val mongoCollection = mongoClient(dbname)(genre)
    val builder = MongoDBObject.newBuilder[String, String]
    builder += "_id" -> id
    builder += "T" -> abc.toAbcMongo.mongoTitles
    builder ++= abc.toAbcMongo.kvs
    builder += TuneModel.tuneKey -> abc.id
    mongoCollection += builder.result
    "Tune " + abc.name + " replaced in " + genre
  }

  def exists(genre: String, id: String) : Boolean = {
    val mongoCollection = mongoClient(dbname)(genre)
    val q = MongoDBObject(TuneModel.tuneKey -> id)
    // val opt = mongoCollection.findOneByID(id)
    val opt = mongoCollection.findOne(q)
    opt.isDefined
  }

  def delete(genre: String, id: String) : Validation[String, String] = withMongoPseudoTransction (mongoDB) {
    val mongoCollection = mongoClient(dbname)(genre)
    val result = mongoCollection.remove(MongoDBObject(TuneModel.tuneKey -> id))
    "Tune " + id + " removed from " + genre
  }

  def delete(genre: String) : Validation[String, String] = withMongoPseudoTransction (mongoDB) {
    val mongoCollection = mongoClient(dbname)(genre)
    val result = mongoCollection.remove(MongoDBObject.empty)
    s"all tunes removed from $genre"
  }


 def getSupportedGenres() : List[String] = {
    val mongoCollection = mongoClient(dbname)("genre")
    val q  = MongoDBObject.empty
    val fields = MongoDBObject("_id" -> 1)

    val iter = for {
      x <- mongoCollection.find(q, fields)
    } yield x.get("_id").asInstanceOf[String]

    iter.toList
  }


  def getSupportedRhythmsFor(genre: String): List[String] = {
    val mongoCollection = mongoClient(dbname)("genre")
    val opt:Option[com.mongodb.DBObject] = mongoCollection.findOneByID(genre)
    opt match {
      case None => {
        // println ("Genre rhythms not found")
        List.empty
      }
      case Some(x) => {
        // println ("Genre rhythms found")
        val untypedList = x.get("rhythms").asInstanceOf[BasicDBList]
        untypedList.collect{ case s: String => s }.toList
      }
    }
  }


  def getTunes(genre: String, sort: String, page: Int, size: Int): Iterator[scala.collection.Map[String, String]] = {
    val params = Map.empty [String, String]
    val sort = "alphabetic"
    search(genre, params, sort, page, size)
  }

  def getNotes(genre: String, id: String): Option[String] =
    getAttribute(genre, id, "abc")

  def getAbcHeaders(genre: String, id: String): Option[String] =
    getAttribute(genre, id, "abcHeaders")

  def getSubmitter(genre: String, id: String): Option[String] =
    getAttribute(genre, id, "submitter")

  private def getAttribute(genre: String, id: String, attribute: String): Option[String] = {
    val mongoCollection = mongoClient(dbname)(genre)
    val q = MongoDBObject(TuneModel.tuneKey -> id)
    val opt:Option[com.mongodb.DBObject] = mongoCollection.findOne(q)
    // val opt:Option[com.mongodb.DBObject] = mongoCollection.findOneByID(id)
    /*
    if (!opt.isDefined) {
      log.debug(s"Not found: tune $id in genre $genre")
    }
    *
    */
    for { o <- opt } yield (o.get(attribute).asInstanceOf[String])
  }

  def getTune(genre: String, tuneid: String): Option[AbcMongo] = {
    val mongoCollection = mongoClient(dbname)(genre)
    // val opt:Option[com.mongodb.DBObject] = mongoCollection.findOneByID(tuneid)
    val q = MongoDBObject(TuneModel.tuneKey -> tuneid)
    val opt:Option[com.mongodb.DBObject] = mongoCollection.findOne(q)
    tuneObjectToAbcMongo(opt, genre)
  }

  /** get the tune reference (i.e. the _id GUID) */
  def getTuneRef(genre: String, id: String): Option[ObjectId] = {
    val mongoCollection = mongoClient(dbname)(genre)
    val q = MongoDBObject(TuneModel.tuneKey -> id)
    val opt:Option[com.mongodb.DBObject] = mongoCollection.findOne(q)
    opt.flatMap(o => o._id)
  }


  /** get the tune by its GUID.  Previously, a tune's GUID was its ID and so the implementation
   *  was identical to that of getTune.  However, we have now refactored the data model
   *  so that we revert to Mongo's own GUIDs for the mongo _id field.
   *
   */
  def getTuneByGUID(genre: String, guid: String): Option[AbcMongo] = {
    val mongoCollection = mongoClient(dbname)(genre)
    val opt:Option[com.mongodb.DBObject] = mongoCollection.findOneByID(guid)
    tuneObjectToAbcMongo(opt, genre)
  }

  /** Convert an optional Mongo Database Object representing a tune to a typesafe
   *  optional AbcMongo object.
   */
  private def tuneObjectToAbcMongo(opto: Option[com.mongodb.DBObject], genre: String) : Option[AbcMongo] =
    opto.flatMap {
      x => {
        // separate out the title list from the other mapped pairs (all strings)
        val untypedValues = x.filter((kv) => kv._1 != "T")
        val stringValues = untypedValues collect { case (k: String, v:String) => (k,v) }
        val titleList = x.get("T").asInstanceOf[BasicDBList]
        val titles = buildScalaList(titleList)
        // println(s"Mongo basic titles $titleList")
        // println(s"Mongo titles $titles")
        Some(AbcMongo(titles, stringValues, genre))
      }
    }

  /** add a new title to a tune */
  def addAlternativeTitle(genre: String, id: String, title: String) : Validation[String, String] = {
    val tuneOption = TuneModel().getTune(genre, id)
    if (title.contains('\\')) {
          (s"Please use Unicode for all titles and don't use backslashes - you have submitted $title").failure[String]
    }
    else if (!tuneOption.isDefined) {
      ("tune " + id + " does not exist").failure[String]
    }
    else {
      // update the T headers within the ABC
      val abcHeaders:String = TuneModel().getAbcHeaders(genre, id).getOrElse("")
      val newAbcHeaders = abcHeaders + ("T: " + title + "\n")
      // update the database
      val mongoCollection = mongoClient(dbname)(genre)
      val addTitleToSet = $addToSet("T" -> title)
      val replaceHeaders = $set("abcHeaders" -> newAbcHeaders)
      val q = MongoDBObject(TuneModel.tuneKey -> id)
      // how does Casbah allow us to do these two operations in a single query?
      mongoCollection.update(q, addTitleToSet)
      mongoCollection.update(q, replaceHeaders)
      // return the valid tune ref
      (s"Tune title ${title} added").success
      }
  }

  /** search the tune store
   *
   * @param genre - the genre of the tune
   * @param params - the search parameters
   * @param sort - the sort parameter (currently 'alpha' or 'data')
   * @param page - the page number
   * @param size - the page size
   *
   */
  def search(genre: String, params: Map[String, String], sort: String, page: Int, size: Int):  Iterator[scala.collection.Map[String, String]] = {
    val mongoCollection = mongoClient(dbname)(genre)
    val q = MongoDBObject.newBuilder
    /* remove the paging and sorting params to leave just the search params */
    params.filterKeys(removePagingAndSortingParam).foreach(p => q+= p._1 -> toRegex(p._2, p._1)  )

    val skip = (page -1) * size
    // CHANGE FOR NEW DB
    // val fields = MongoDBObject(TuneModel.tuneKey -> 1, "ts" -> 2, "T" -> 3)
    // for tunebank-frontend
    val fields = MongoDBObject(TuneModel.tuneKey -> 1, "ts" -> 2, "T" -> 3, "abcHeaders" -> 4, "abc" -> 5)   

    val s = if (sort == "date")
      MongoDBObject("ts" -> -1)
    else
      MongoDBObject(TuneModel.tuneKey -> 1)

    for {
      x <- mongoCollection.find(q.result, fields).sort(s).skip(skip).limit(size)
    } yield {
        val untypedValues = x.filter((kv) => kv._1 != "T")
        val stringValues = untypedValues collect { case (k: String, v:String) => (k,v) }
        val titleList = x.get("T").asInstanceOf[BasicDBList]
        val titles = buildScalaList(titleList)
        val otherTitles = alternativeTitles(titles)
        otherTitles match {
          case None => stringValues
          case Some(t) => stringValues + t
        }
    }
  }


  def count(genre: String, params: Map[String, String]) : Long  = {
    val mongoCollection = mongoClient(dbname)(genre)
    val q = MongoDBObject.newBuilder
    /* remove the paging and sorting params to leave just the search params */
    params.filterKeys(removePagingAndSortingParam).foreach(p => q+= p._1 -> toRegex(p._2, p._1)  )
    mongoCollection.count(q.result)
  }

   /** add a unique index on the genre */
  def createIndex(genre: String) = {
     // don't make one if we're using the default _id key - Mongo does this automatically
     if ("_id" != TuneModel.tuneKey) {
       val mongoCollection = mongoClient(dbname)(genre)
       /* mongoCollection.ensureIndex( DBObject(TuneModel.tuneKey -> 1), DBObject("unique" -> true) ) */
       mongoCollection.createIndex( MongoDBObject(TuneModel.tuneKey -> 1), "tuneKey", true)
       log.info(s"unique index ${TuneModel.tuneKey} created on ${genre}")
     }
  }

  /* implementation (not exposed)*/

  /** Build a case-insensitive regex pattern from the match string.
   * e.g. Reel becomes ^[Rr][Ee][Ee][Ll]
   * (T (title), Z (transcription notes) and abc (notes) search the entire string)
   * all others are anchored to the string start
   */
  private def toRegex(s:String, context:String): scala.util.matching.Regex = {
    val ds = java.net.URLDecoder.decode(s, "UTF-8")
    val basicPattern = ds.foldLeft("") ( (b, c) => if (c.isLetter) b + "[" + c.toUpper + c.toLower + "]" else b + c)
    val pattern = context match {
      case "T" | "Z" | "abc" => basicPattern
      case _ => "^" + basicPattern
    }
    println("regex pattern: " + pattern)
    pattern.r
  }


  /** build a scala list from a Mongo list */
  private def buildScalaList(from: MongoDBList) : List[String] = {
      val lseq = from collect {case s:String => s}
      lseq.toList
  }

  /* provide a hint of the alternative tune titles */
  private def alternativeTitles(titles: List[String]): Option[(String, String)] =
    titles.length match {
      case 0 => None
      case 1 => None
      case 2 => Some(   ("otherTitles", "(" + titles.tail.head.toLowerCase + ")"  ) )
      case _ => Some(   ("otherTitles", "(" + titles.tail.head.toLowerCase  + "...)"   ) )
  }

  /** (MongoDBList.toList throws a null pointer exception)
  private def buildScalaList(from: MongoDBList) : List[String] = {
    val untypedList = from.toList
    untypedList  collect {case s:String => s}
  }
  */

  /* no longer used - but these show some of the search keys that we expect
   private def allowedSearchParam(p: String): Boolean = {
     println(s"filter key: ${p}")
     List(TuneModel.tuneKey, "T", "O", "M", "Q", "Z", "K", "R", "abc", "submitter").contains(p)
   }
   *
   */

  /* Unfortunately, the search params come bundled with the paging and sorting params
   *
   * Filter them out
   * */
  private def removePagingAndSortingParam(p: String): Boolean =
    (p != "page" && p != "sort")



}
