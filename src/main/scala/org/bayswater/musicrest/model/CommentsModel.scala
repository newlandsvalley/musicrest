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

import org.bayswater.musicrest.MusicRestSettings
import com.mongodb.casbah.Imports._
import com.mongodb.ServerAddress
import scalaz.Validation


trait CommentsModel { 

  /** insert a comment  */
  def insertComment(genre: String, tune: String, comment: Comment): Validation[String, String] 

  /** get a comment by key*/
  def getComment(genre: String, tune: String, user: String, cid: String) : Validation[String, Comment]

  /** delete a comment by key */
  def deleteComment(genre: String, tuneId: String, user: String, cid: String) : Validation[String, String] 
  
  /** delete all comments for a genre */
  def deleteAllComments(genre: String) : Validation[String, String] 
  
  /** delete all comments for a genre and tune */
  def deleteComments(genre: String, tuneId: String) : Validation[String, String]

  /** get all comments */
  def getComments(genre: String, tune: String) : Seq[Comment]

}

object CommentsModel {    

  private val musicRestSettings = MusicRestSettings
  private val mongoOptions = MongoClientOptions ( connectionsPerHost = musicRestSettings.dbPoolSize )
  private val mongoClient = MongoClient(new ServerAddress(musicRestSettings.dbHost,  musicRestSettings.dbPort), mongoOptions)
  private val commentsModel = new CommentsModelNormalisedCasbahImpl(mongoClient, musicRestSettings.dbName)
  // private val port = 27017
  // private val host = "localhost"
  def apply(): CommentsModel = commentsModel
}
