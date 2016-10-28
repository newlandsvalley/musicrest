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
import org.bayswater.musicrest.abc.SupportedGenres

class CommentsModelNormalisedCasbahImpl(val mongoClient: MongoClient, val dbname: String) extends CommentsModel with MongoTransaction {  
  
  val log = implicitly[LoggingContext]
  val mongoDB = mongoClient(dbname) 
  val COMMENTS = "comments"
    
  val before = for (g <- SupportedGenres.genres) {
    createIndexes(g)
  } 
  
  // set the write concern to Safe
  val writeConcern = {
    mongoDB.setWriteConcern(WriteConcern.Safe)
    mongoDB.getWriteConcern
  }

  
  def createIndexes(genre: String) {
    val mongoCollection = mongoClient(dbname)(genre + COMMENTS)
    mongoCollection.createIndex( MongoDBObject("tid" -> 1, "user" -> 2, "cid" -> 3), "commentRef", true )
    log.info(s"unique index created for ${genre + COMMENTS}") 
    mongoCollection.createIndex( MongoDBObject("tid" -> 1) )
    log.info(s"non-unique index created on tid for ${genre + COMMENTS}")
  }

  
 
  def insertComment(genre: String, tuneId: String, comment: Comment): Validation[String, String] = withMongoPseudoTransction (mongoDB) { 
    log.info(s"Insert request for ${tuneId}, ${comment.user}, ${comment.cid}")    
    // println(s"Insert request for ${tuneId}, ${comment.user}, ${comment.cid}")   
    val optTuneRef = TuneModel().getTuneRef(genre, tuneId)
    if (!optTuneRef.isDefined) {
      s"Not found genre: ${genre} tune: ${tuneId}"
    }
    else {
      if (existsComment(genre, optTuneRef.get, comment.user, comment.cid)) {
        // println(s"exists already ${tuneId}")
        if (updateComment(genre, optTuneRef.get, comment)) {        
          s"Comment updated user: ${comment.user} id: ${comment.cid} for tune ${tuneId}"
        }
        else {
          // caught by withMongoPseudoTransaction and issued as a validation failure
          throw new RuntimeException(s"Comment update failed user: ${comment.user} id: ${comment.cid} for tune ${tuneId}")
        }
      }
      else {
        // println(s"doesn't exist already ${tuneId}")
        val mongoCollection = mongoClient(dbname)(genre + COMMENTS)
        val builder = MongoDBObject.newBuilder[String, String]
        builder += "tid" -> optTuneRef.get
        builder ++= comment.toMap
        mongoCollection += builder.result
        s"Comment inserted user: ${comment.user} id: ${comment.cid} for tune ${tuneId}"
      }
    }
  }

  /** get a comment by key */
  def getComment(genre: String, tuneId: String, user: String, cid: String) : Validation[String, Comment] =  {  
    log.info(s"Get request for ${tuneId}, ${user}, ${cid}")
    val optTuneRef = TuneModel().getTuneRef(genre, tuneId)
    if (!optTuneRef.isDefined) {
      (s"Not found genre: ${genre} tune: ${tuneId}").failure[Comment]
    }
    else {
      val mongoCollection = mongoClient(dbname)(genre + COMMENTS)
      val q = MongoDBObject("user" -> user, "cid" -> cid, "tid" -> optTuneRef.get) 
      val opt:Option[com.mongodb.DBObject] = mongoCollection.findOne(q)
      opt.map( c => Comment(c).success).getOrElse(s"Comment not found for tune ${tuneId} and user ${user} and id ${cid}".failure[Comment])
    }
  }

  /** delete a comment by key */
  def deleteComment(genre: String, tuneId: String, user: String, cid: String) : Validation[String, String] = withMongoPseudoTransction (mongoDB) {
    log.info(s"Delete request for ${tuneId}, ${user}, ${cid}")
    val optTuneRef = TuneModel().getTuneRef(genre, tuneId)
    if (!optTuneRef.isDefined) {
      s"Not found genre: ${genre} tune: ${tuneId}"
    }
    else {
      val mongoCollection = mongoClient(dbname)(genre + COMMENTS)
      val q = MongoDBObject("user" -> user, "cid" -> cid, "tid" -> optTuneRef.get) 
      val result = mongoCollection.remove(q)
      s"Comment deleted user: ${user} id: ${cid} for tune: ${tuneId}" 
    }
  } 
  
   /** delete all comments for a genre */
  def deleteAllComments(genre: String) : Validation[String, String] = withMongoPseudoTransction (mongoDB) {
    val mongoCollection = mongoClient(dbname)(genre + COMMENTS)    
    val result = mongoCollection.remove(MongoDBObject.empty)
    s"All comments removed for genre ${genre}"   
  }
  
  /** delete all comments for a genre and tune */
  def deleteComments(genre: String, tuneId: String) : Validation[String, String]  = withMongoPseudoTransction (mongoDB) {  
    log.info(s"Delete request for comments of ${genre} : ${tuneId}")
    val optTuneRef = TuneModel().getTuneRef(genre, tuneId)
    if (!optTuneRef.isDefined) {
      s"Not found genre: ${genre} tune: ${tuneId}"
    }
    else {
      val mongoCollection = mongoClient(dbname)(genre + COMMENTS)     
      val q = MongoDBObject("tid" -> optTuneRef.get)  
      val result = mongoCollection.remove(q)
      s"All comments deleted for genre: ${genre} and tune: ${tuneId}"   
    }
  }


  /** get all comments (will be empty naturally if the tune does not exist) */
  def getComments(genre: String, tuneId: String) : Seq[Comment] = {
    val optTuneRef = TuneModel().getTuneRef(genre, tuneId)
    val tuneRef = optTuneRef.getOrElse(new ObjectId(new java.util.Date()))
    val mongoCollection = mongoClient(dbname)(genre + COMMENTS)
    val q = MongoDBObject("tid" -> tuneRef)  
    val s = MongoDBObject("cid" -> -1)
    val fields = MongoDBObject("user" -> 1,  "cid" -> 2, "subject" -> 3, "text" -> 4)  
    val comments = for {
      x <- mongoCollection.find(q, fields).sort(s)
    } yield {
       Comment(x)
    }
    comments.toList
  }

  /* implementation */
  
  private def existsComment(genre: String, tuneRef: ObjectId, user: String, cid: String): Boolean = {
    val mongoCollection = mongoClient(dbname)(genre + COMMENTS)
    val q = MongoDBObject("tid" -> tuneRef, "user" -> user, "cid" -> cid) 
    val opt:Option[com.mongodb.DBObject] = mongoCollection.findOne(q)
    opt.isDefined
  }
  
  def updateComment(genre: String, tuneRef: ObjectId, comment: Comment): Boolean = {     
    // println(s"updating comment - title: ${comment.title} text: ${comment.text} ")
    val mongoCollection = mongoClient(dbname)(genre + COMMENTS)
    val q = MongoDBObject("tid" -> tuneRef, "user" -> comment.user, "cid" -> comment.cid) 
    val opt:Option[com.mongodb.DBObject] = mongoCollection.findOne(q)
    opt match {
      case Some(userDBObject) => {
                               val set = $set("subject" -> comment.subject, "text" -> comment.text)
                               mongoCollection.update(userDBObject, set)
                               true
                               }
      case None => false        
    }
  }
  
 
}
