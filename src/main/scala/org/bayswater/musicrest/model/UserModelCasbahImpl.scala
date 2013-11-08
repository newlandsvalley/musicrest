package org.bayswater.musicrest.model

import spray.util.LoggingContext  
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.MongoDB
import scalaz.Validation
import scalaz.syntax.validation._
import scala.collection.JavaConversions._

class UserModelCasbahImpl (val mongoConnection: MongoConnection, val dbname: String) extends UserModel with MongoTransaction {  
  
  val log = implicitly[LoggingContext]
  val mongoDB = MongoDB(mongoConnection, dbname) 
  
  // set the write concern to Safe
  val writeConcern:WriteConcern = {
    mongoDB.setWriteConcern(WriteConcern.Safe)
    mongoDB.getWriteConcern
  }
  
  def existsUser(id: String) : Boolean = {
    val mongoCollection = mongoConnection(dbname)("users")
    val opt = mongoCollection.findOneByID(id)
    opt.isDefined    
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
    else withMongoPseudoTransction (mongoDB) {
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
  
  def userCount(): Long =  {
    val mongoCollection = mongoConnection(dbname)("users")
    val everything = MongoDBObject.empty
    mongoCollection.count(everything)
  }

  def deleteUser(id: String) : Validation[String, String] = withMongoPseudoTransction (mongoDB) {
    val mongoCollection = mongoConnection(dbname)("users")
    val result = mongoCollection.remove(MongoDBObject("_id" -> id))
    "User " + id + " removed"   
  }   
  
  def deleteUsers : Validation[String, String] = withMongoPseudoTransction (mongoDB) {
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
    
}