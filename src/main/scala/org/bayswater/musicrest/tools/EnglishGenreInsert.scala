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

/* This is here simply because the English genre was added retrospectively. This
 * worked OK but unaccountably the index creation failed because of lack of 
 * authorisation.  It was instead created through the MongoDB shell.
 *
 * It is temporary and will probably be removed in subsequent releases.
 * 
 */

package org.bayswater.musicrest.tools


import com.mongodb.casbah.Imports._
import org.bayswater.musicrest.model._

object EnglishGenreInsert {

  def main(args: Array[String]): Unit = {
    if (args.length < 4) {
      println("Usage: EnglishGenreInsert <host> <user> <password> <db name>")
      System.exit(0)
    }

    val dbHost = args(0)
    val dbUser = args(1)
    val dbPassword = args(2)
    val dbName = args(3)

    insertEnglishGenre(dbHost, dbUser, dbPassword, dbName)
    ensureIndex(dbName, "english")
  }

  def insertEnglishGenre(dbHost: String, dbUser: String, dbPassword: String, dbName: String) {
    val collectionName = "genre"

    // use default port for the time being
    val mongoClient = MongoCasbahUtil.buildMongoClient(dbHost,  27017, dbUser, dbPassword, dbName)
    val mongoCollection = mongoClient (dbName) (collectionName)

    insert(mongoCollection, "english", MongoDBList("jig","reel","hornpipe","march","minuet","polka","three-two","waltz"))
        
  }

  def insert(mongoCollection: MongoCollection, genre: String, rhythms: MongoDBList) {
    println ("inserting " + genre)
    val builder = MongoDBObject.newBuilder[String, Any]
    builder += "_id" -> genre
    builder += "rhythms" -> rhythms
    // val obj = MongoDBObject("rhythms" -> rhythms)
    mongoCollection += builder.result
  }

   private def ensureIndex(dbName: String, genre: String) {
    println("insuring index on english")
    val tuneModel = new TuneModelCasbahImpl(MongoClient(), dbName)
    tuneModel.createIndex(genre)
  }
}
