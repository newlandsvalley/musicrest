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

import org.bayswater.musicrest.model.MongoCasbahUtil
import com.mongodb.casbah.Imports._

object UserInsert {

 def main(args: Array[String]): Unit = {
    if (args.length < 7) {
      println("Usage: UserInsert <host> <db user> <db password> <db name> <user name> <password> <email>")
      System.exit(0)
    }

    val dbHost = args(0)
    val dbUser = args(1)
    val dbPassword = args(2)
    val dbName = args(3)
    val uName = args(4)
    val password = args(5)
    val email = args(6)

    insertUser(dbHost, dbUser, dbPassword, dbName, uName, password, email)

    def insertUser(dbHost: String, dbUser: String, dbPassword: String, dbName: String, uName: String, password: String, email: String) {
      val collectionName = "users"

      // use default port for the time being
      val mongoClient = MongoCasbahUtil.buildMongoClient(dbHost,  27017, dbUser, dbPassword, dbName)

      val mongoCollection = mongoClient (dbName) (collectionName)
      println ("inserting " + uName)
      val builder = MongoDBObject.newBuilder[String, String]
      builder += "_id" -> uName
      builder += "email" -> email
      builder += "password" -> password
      builder += "valid" -> "Y"
      mongoCollection += builder.result
      "User: " + uName + " added"
    }
  }
}
