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

import org.bayswater.musicrest.abc.AbcMongo
import org.bayswater.musicrest.abc.Abc
import org.bayswater.musicrest.MusicRestSettings
import com.mongodb.casbah.Imports._
import com.mongodb.ServerAddress
import scalaz.Validation


trait TuneModel { 

  def testConnection(): Boolean
  
  /** insert a tune */
  def insert(genre: String, abc:Abc): Validation[String, String]  
  
  /** replace an existing tune (i.e. one with an existing tune _id) */
  def replace(id: ObjectId, genre: String, abc:Abc): Validation[String, String] 
  
  /** search by if to discover if the tune exists */
  def exists(genre: String, id: String) : Boolean
  
  /** delete a tune by id */
  def delete(genre: String, id: String) : Validation[String, String]

  /** delete all the tunes within the genre */
  def delete(genre: String) : Validation[String, String]
  
  /** get all the currently supported genres */
  def getSupportedGenres() : List[String]
  
  /** get all the currently supported rhythms for the supplied genre */
  def getSupportedRhythmsFor(genre: String): List[String]
  
  /** get a page from the complete set of tunes in the genre */
  def getTunes(genre: String, sort: String, page: Int, size: Int):  Iterator[scala.collection.Map[String, String]]
  
  /** get the ABC notes for the supplied tune */
  def getNotes(genre: String, id: String): Option[String]     
  
  /** get the ABC headers for the supplied tune */
  def getAbcHeaders(genre: String, id: String): Option[String]   
  
  /** get the submitter of the tune */
  def getSubmitter(genre: String, id: String): Option[String]  
  
  /** get the tune */
  def getTune(genre: String, id: String): Option[AbcMongo]   
  
  /** get the tune reference (i.e. the _id GUID) */
  def getTuneRef(genre: String, id: String): Option[ObjectId]  
  
  /** add a new title to a tune */ 
  def addAlternativeTitle(genre: String, id: String, title: String) : Validation[String, String]  

  /** generic search */
  def search(genre: String, params: Map[String, String], sort: String, page: Int, size: Int):  Iterator[scala.collection.Map[String, String]]  

  /** results count for a generic search */
  def count(genre: String, params: Map[String, String]) : Long  

  /** add an index on the genre */
  def createIndex(genre: String)
}

object TuneModel {  
  
  /* Potential change for new DB representation for tunes.
   * 
   * By default, Mongo automatically provides _id as a GUID which is implicit.  i.e. if we use tid as our unique id, then _id is also there.
   * Up to version 1.1.2, we over-ride Mongos's _id with out own value (a concatenation of tune name and rhythm)
   * From version 1.1.3 we intend to revert to using Mongo's _id and supply our tid key in addition
   * This is the only line we need to alter to change the DB tune representation
   */
  val tuneKey = "tid"
  // val tuneKey = "_id"
  /** MongoConnection is very badly documented in Casbah.  Apparently it is in fact a pooled connection and
   *  you can alter the size of the pool with MongoOptions.  (This is raw MongoDB behaviour).  Let's
   *  experiment with just setting the pool size for the moment. 
   */
  private val musicRestSettings = MusicRestSettings
  private val mongoOptions = MongoClientOptions ( connectionsPerHost = musicRestSettings.dbPoolSize )
  private val mongoClient = MongoClient(new ServerAddress(musicRestSettings.dbHost,  musicRestSettings.dbPort), mongoOptions)
  private val casbahTuneModel = new TuneModelCasbahImpl(mongoClient, musicRestSettings.dbName)
  // private val port = 27017
  // private val host = "localhost"
  def apply(): TuneModel = casbahTuneModel
}
