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
import scalaz.Validation


trait TuneModel { 

  def testConnection(): Boolean
  
  /** insert a tune */
  def insert(genre: String, abc:Abc): Validation[String, String] 
  
  /** search by if to discover if the tune exists */
  def exists(genre: String, id: String) : Boolean
  
  /** delete a tune by id */
  def delete(genre: String, id: String) : Validation[String, String]

  /** delete all the tunes within the genre */
  def delete(genre: String) : Validation[String, String]
  
  /** see if the user exists */
  def existsUser(id: String) : Boolean
  
  /** get all the currently supported genres */
  def getSupportedGenres() : List[String]
  
  /** get all the currently supported rhythms for the supplied genre */
  def getSupportedRhythmsFor(genre: String): List[String]
  
  /** get a page from the complete set of tunes in the genre */
  def getTunes(genre: String, sort: String, page: Int, size: Int):  Iterator[scala.collection.Map[String, String]]
  
  /** get the ABC notes for the supplied tune */
  def getNotes(genre: String, id: String): Option[String]   
  
  /** get the submitter of the tune */
  def getSubmitter(genre: String, id: String): Option[String]  
  
  /** get the tune */
  // def getTune(genre: String, id: String): Option[scala.collection.Map[String,String]]  
  def getTune(genre: String, id: String): Option[AbcMongo]  
  
  /** add a new title to a tune */ 
  def addAlternativeTitle(genre: String, id: String, title: String) : Validation[String, String]  

  /** generic search */
  def search(genre: String, params: Map[String, String], sort: String, page: Int, size: Int):  Iterator[scala.collection.Map[String, String]]  
  
  /** insert a (not yet validated) user into the database */
  def insertUser(user: User): Validation[String, UnregisteredUser]
  
  /** insert a user and set his registration status */
  def insertUser(user: User, isRegistered: Boolean): Validation[String, UnregisteredUser] 
  
  /** insert a new user who'se automatically registered */
  def insertPreRegisteredUser(user: User): Validation[String, UnregisteredUser]  
  
  /** get the user details */
  def getUser(name: String) : Validation[String, UserRef]
  
  /** set the user's validity */
  def validateUser(uuid: String): Validation[String, UnregisteredUser] 
  
  /** get a list of users */
  def getUsers(page: Int, size: Int): Iterator[UserRef]
  
  /** check the incoming credentials to see if this is a valid user */
  def isValidUser(name: String, password:String): Boolean 
  
  /** results count for a generic search */
  def count(genre: String, params: Map[String, String]) : Long  
  
  /** count users */
  def userCount() : Long
  
  /** alter a user's password */
  def alterPassword(name: String, password: String) : Validation[String, String]

  /** delete user */
  def deleteUser(id: String) : Validation[String, String] 
  
  /** delete all users */
  def deleteUsers : Validation[String, String]  
}

object TuneModel {  
  private val musicRestSettings = MusicRestSettings
  // private val port = 27017
  // private val host = "localhost"
  def apply(): TuneModel = new TuneModelCasbahImpl(MongoConnection(musicRestSettings.dbHost,  musicRestSettings.dbPort), musicRestSettings.dbName)
}
