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
import org.bayswater.musicrest.abc.AbcMongo
import org.bayswater.musicrest.abc.Abc
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.MongoDB
import scalaz.Validation
import scalaz.syntax.validation._
import scala.collection.JavaConversions._

class TuneModelCasbahImpl(val mongoConnection: MongoConnection, val dbname: String) extends TuneModel {  
  
  val log = implicitly[LoggingContext]
  val mongoDB = MongoDB(mongoConnection, dbname) 
 
  // set the write concern to Safe
  val writeConcern:WriteConcern = {
    mongoDB.setWriteConcern(WriteConcern.Safe)
    mongoDB.getWriteConcern
  }

  def testConnection(): Boolean = try {     
    userCount
    true   
  }
  catch {
    case t: Throwable => log.error("Couldn't connect to users collection on database " + dbname)
    false
  }
 
  def insert(genre: String, abc:Abc): Validation[String, String] = withMongoPseudoTransction {
    val mongoCollection = mongoConnection(dbname)(genre)
    val builder = MongoDBObject.newBuilder[String, String]
    builder += "T" -> abc.toAbcMongo.mongoTitles
    builder ++= abc.toAbcMongo.kvs    
    builder += TuneModel.tuneKey -> abc.id
    mongoCollection += builder.result
    "Tune " + abc.name + " inserted into " + genre
  } 
  
  def exists(genre: String, id: String) : Boolean = {
    val mongoCollection = mongoConnection(dbname)(genre)
    val q = MongoDBObject(TuneModel.tuneKey -> id) 
    // val opt = mongoCollection.findOneByID(id)
    val opt = mongoCollection.findOne(q)
    opt.isDefined
  }   
  
  def delete(genre: String, id: String) : Validation[String, String] = withMongoPseudoTransction {
    val mongoCollection = mongoConnection(dbname)(genre)
    val result = mongoCollection.remove(MongoDBObject(TuneModel.tuneKey -> id))
    "Tune " + id + " removed from " + genre    
  }   

  def delete(genre: String) : Validation[String, String] = withMongoPseudoTransction {
    val mongoCollection = mongoConnection(dbname)(genre)
    val result = mongoCollection.remove(MongoDBObject.empty)
    s"all tunes removed from $genre"
  }   
  
  def existsUser(id: String) : Boolean = {
    val mongoCollection = mongoConnection(dbname)("users")
    val opt = mongoCollection.findOneByID(id)
    opt.isDefined    
  }   
  
 def getSupportedGenres() : List[String] = {
    val mongoCollection = mongoConnection(dbname)("genre")
    val q  = MongoDBObject.empty
    val fields = MongoDBObject("_id" -> 1)    
       
    val iter = for {
      x <- mongoCollection.find(q, fields)
    } yield x.get("_id").asInstanceOf[String]
    
    iter.toList
  }  
  

  def getSupportedRhythmsFor(genre: String): List[String] = {
    val mongoCollection = mongoConnection(dbname)("genre")
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
  
  def getSubmitter(genre: String, id: String): Option[String] = 
    getAttribute(genre, id, "submitter")    
    
  private def getAttribute(genre: String, id: String, attribute: String): Option[String] = {
    val mongoCollection = mongoConnection(dbname)(genre)
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
    val mongoCollection = mongoConnection(dbname)(genre)
    // val opt:Option[com.mongodb.DBObject] = mongoCollection.findOneByID(tuneid)
    val q = MongoDBObject(TuneModel.tuneKey -> tuneid) 
    val opt:Option[com.mongodb.DBObject] = mongoCollection.findOne(q)
    tuneObjectToAbcMongo(opt, genre)
  }  
  
  /** get the tune by its GUID.  Currently a tune's GUID is its ID and so this implementation 
   *  can be identical to that of getTune.  However, if we refactor the data model
   *  so that we revert to Mongo's own GUIDs for the mongo _id field, these implementations
   *  will differ
   */
  def getTuneByGUID(genre: String, guid: String): Option[AbcMongo] = {
    val mongoCollection = mongoConnection(dbname)(genre)
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
    if (!tuneOption.isDefined) {
      ("tune " + id + " does not exist").fail
    }
    else {
      val abcMongo = tuneOption.get
      val abc = Abc(abcMongo)
      val newAbc = abc.addAlternativeTitle(title)
      newAbc.flatMap( a => TuneModel().insert(genre, a) )
      }
  }
    
  
  def insertUser(user: User): Validation[String, UnregisteredUser] = insertUser(user, false) 
  
  /** get the user details */
  def getUser(name: String) : Validation[String, UserRef] = {
    println("looking for user " + name)    
    val opt = getUserByName(name)
    opt match {
      case Some(userDBObject) => {
                               val email = userDBObject.get("email").asInstanceOf[String]
                               val password = userDBObject.get("password").asInstanceOf[String]
                               UserRef(name, email, password).success
                               }
      case None => "No such user".fail
    } 
  }
  
  /** insert a user who's automatically registered */
  def insertPreRegisteredUser(user: User): Validation[String, UnregisteredUser] = insertUser(user, true) 
  
  def insertUser(user: User, isRegistered: Boolean): Validation[String, UnregisteredUser] = {
    if (existsUser(user.name)) {
      ("User: " + user.name + " already exists").fail
    }
    else withMongoPseudoTransction {
      val validity = if (isRegistered) "Y" else "N"
      val mongoCollection = mongoConnection(dbname)("users")
      val builder = MongoDBObject.newBuilder[String, String]
      builder += "_id" -> user.name
      builder += "email" -> user.email
      builder += "password" -> user.password
      builder += "uuid" -> user.uuid
      builder += "valid" -> validity
      mongoCollection += builder.result
      UnregisteredUser(user.name, user.email, user.password, user.uuid)
    }
  }  
 
  
  def validateUser(uuid: String): Validation[String, UnregisteredUser] = {
    val opt = getUserByUuid(uuid)
    opt match {
      case Some(userDBObject) => {
                               validateTheUser(userDBObject)
                               val uid = userDBObject.get("_id").asInstanceOf[String]
                               val email = userDBObject.get("email").asInstanceOf[String]
                               val password = userDBObject.get("password").asInstanceOf[String]
                               UnregisteredUser(uid, email, password, uuid).success
                               }
      case None => "No such user".fail
    }
  } 
 
  def isValidUser(name: String, password:String): Boolean = {
    // println(s"looking for user: $name and password $password in $dbname")
    val mongoCollection = mongoConnection(dbname)("users")
    val q = MongoDBObject.newBuilder[String, String]
    q += "_id" -> name
    q += "password" -> password
    q += "valid" -> "Y"  
    val opt = mongoCollection.findOne(q.result)
    // println(s"result: $opt")
    opt.isDefined
  }
   
  /** get a list of users */
  def getUsers(page: Int, size: Int): Iterator[UserRef] = {    
    val mongoCollection = mongoConnection(dbname)("users")
    val fields = MongoDBObject("_id" -> 1, "email" -> 2, "password" -> 3)
    val everything = MongoDBObject.empty 
    val skip = (page -1) * size
    val rows = for {
      x <- mongoCollection.find(everything, fields).skip(skip).limit(size)
    } yield x.mapValues(v => v.asInstanceOf[String])        
    rows map { x => 
      val name = x.getOrElse("_id", "no name")
      val email = x.getOrElse("email", "no email")
      val password = x.getOrElse("password", "no password")
      UserRef(name, email, password)
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
    val mongoCollection = mongoConnection(dbname)(genre)
    val q = MongoDBObject.newBuilder
    params.filterKeys(allowedSearchParam).foreach(p => q+= p._1 -> toRegex(p._2, p._1)  ) 
    // params.foreach(p => q+= p)
    
    val skip = (page -1) * size
    // val fields = MongoDBObject("T" -> 1, "R" -> 2, "ts" -> 3) 
    // CHANGE FOR NEW DB
    val fields = MongoDBObject(TuneModel.tuneKey -> 1, "ts" -> 2, "T" -> 3)     
    //val fields = MongoDBObject("ts" -> 1, "T" -> 2)    
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
  
  /*  old version where we don't project alternative titles
  def search(genre: String, params: Map[String, String], sort: String, page: Int, size: Int):  Iterator[scala.collection.Map[String, String]] = {
    val mongoCollection = mongoConnection(dbname)(genre)
    val q = MongoDBObject.newBuilder
    params.filterKeys(allowedSearchParam).foreach(p => q+= p._1 -> toRegex(p._2, p._1)  ) 
    // params.foreach(p => q+= p)
    
    val skip = (page -1) * size
    // val fields = MongoDBObject("T" -> 1, "R" -> 2, "ts" -> 3)   
    val fields = MongoDBObject("ts" -> 1)    
    val s = if (sort == "date")
      MongoDBObject("ts" -> -1)
    else
      MongoDBObject("_id" -> 1)
      
    for {
      x <- mongoCollection.find(q.result, fields).sort(s).skip(skip).limit(size)
    } yield x.mapValues(v => v.asInstanceOf[String])        
  }  
  */  
  
  def count(genre: String, params: Map[String, String]) : Long  = {
    val mongoCollection = mongoConnection(dbname)(genre)
    val q = MongoDBObject.newBuilder
    params.filterKeys(allowedSearchParam).foreach(p => q+= p._1 -> toRegex(p._2, p._1)  )    
    mongoCollection.count(q.result)
  }
  
  
  def userCount(): Long =  {
    val mongoCollection = mongoConnection(dbname)("users")
    val everything = MongoDBObject.empty
    mongoCollection.count(everything)
  }

  def deleteUser(id: String) : Validation[String, String] = withMongoPseudoTransction {
    val mongoCollection = mongoConnection(dbname)("users")
    val result = mongoCollection.remove(MongoDBObject("_id" -> id))
    "User " + id + " removed"   
  }   
  
  def deleteUsers : Validation[String, String] = withMongoPseudoTransction {
    val mongoCollection = mongoConnection(dbname)("users")
    val result = mongoCollection.remove(MongoDBObject.empty)
    "All users removed"   
  }    
  
  def alterPassword(name: String, password: String) : Validation[String, String] = {
    val mongoCollection = mongoConnection(dbname)("users")
    val opt = getUserByName(name)
    opt match {
      case Some(userDBObject) => {
                               val set = $set("password" -> password)
                               mongoCollection.update(userDBObject, set)
                               (name + ": password changed").success
                               }
      case None => "No such user".fail
    }
  }    
  
  /* implementation (not exposed)*/
  
  private def getUserByUuid(uuid: String) : Option[com.mongodb.DBObject] = {
    val mongoCollection = mongoConnection(dbname)("users")
    val q  = MongoDBObject.newBuilder
    q += "uuid" -> uuid    
    mongoCollection.findOne(q.result)
  }   
  
  private def getUserByName(name: String) : Option[com.mongodb.DBObject] = {
    val mongoCollection = mongoConnection(dbname)("users")
    val q  = MongoDBObject.newBuilder
    q += "_id" -> name   
    mongoCollection.findOne(q.result)
  }   
  
  private def validateTheUser(user: com.mongodb.DBObject) { 
    val mongoCollection = mongoConnection(dbname)("users")   
    mongoCollection.update(user, $set("valid" -> "Y"))
  }  
  
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
  
  private def allowedSearchParam(p: String): Boolean = List(TuneModel.tuneKey, "T", "O", "M", "Q", "Z", "K", "R", "abc", "submitter").contains(p)
  
  /** Apparently, Mongo doesn't do transactions - this is the nearest we get - it ties the sequence of statements
   * to one particular Mongo connection and (we hope) raises an exception against that connection if things go wrong.
   */
  private def withMongoPseudoTransction [A] (body:  =>  A) : Validation[String, A] = 
    try {
      mongoDB.requestStart
      val b = body
      mongoDB.getLastError.throwOnError
      b.success
    }
    catch {
       case e: Exception  => e.getMessage().fail      
    }
    finally {
      mongoDB.requestDone      
    }
}

