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

package org.bayswater.musicrest.tools


import com.mongodb.casbah.Imports._
import org.bayswater.musicrest.model._

object GenreInsert {

  def main(args: Array[String]): Unit = {
    if (args.length < 4) {
      println("Usage: GenreInsert <host> <user> <password> <db name>")
      System.exit(0)
    }

    val dbHost = args(0)
    val dbUser = args(1)
    val dbPassword = args(2)
    val dbName = args(3)

    insertAllGenres(dbHost, dbUser, dbPassword, dbName)
    ensureIndexes(dbName, List("irish", "scottish", "scandi", "klezmer"))
  }

  def insertAllGenres(dbHost: String, dbUser: String, dbPassword: String, dbName: String) {
    val collectionName = "genre"

    // use default port for the time being
    val mongoClient = MongoCasbahUtil.buildMongoClient(dbHost,  27017, dbUser, dbPassword, dbName)
    val mongoCollection = mongoClient (dbName) (collectionName)

    insert(mongoCollection, "irish", MongoDBList("jig","reel","hornpipe","barndance","highland","march","mazurka","polka","slide","slip jig","waltz"))
    insert(mongoCollection, "scottish", MongoDBList("jig","reel","hornpipe","barndance","march","schottische","slip jig","strathspey","waltz"))
    insert(mongoCollection, "scandi", MongoDBList("polska","brudmarsch","gånglåt","skänklåt","slängpolska","polka","långdans","marsch","schottis","engelska",
                       "halling","hambo","sekstur", "waltz"))
    insert(mongoCollection, "klezmer", MongoDBList("bulgar","freylekhs","khosidl","hora", "csardas","doina", "honga", "hopak","kasatchok",
                             "kolomeyke","sher","sirba","skotshne","taksim","terkish"))
  }

  def insert(mongoCollection: MongoCollection, genre: String, rhythms: MongoDBList) {
    println ("inserting " + genre)
    val builder = MongoDBObject.newBuilder[String, Any]
    builder += "_id" -> genre
    builder += "rhythms" -> rhythms
    // val obj = MongoDBObject("rhythms" -> rhythms)
    mongoCollection += builder.result
    println("insuring index on " + genre)
  }

   private def ensureIndexes(dbName: String, genres: List[String]) {
    val tuneModel = new TuneModelCasbahImpl(MongoClient(), dbName)
    genres.foreach {g => tuneModel.createIndex(g) }
  }
}
