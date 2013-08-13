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

object UserInsert {
  
 def main(args: Array[String]): Unit = {
    if (args.length < 4) {      
      println("Usage: UserInsert <db name> <user name> <password> <email>")
      System.exit(0)
    }    
    
    val dbName = args(0)
    val uName = args(1)    
    val password = args(2)
    val email = args(3)
    
    insertUser(dbName, uName, password, email)
    
    def insertUser(dbName: String, uName: String, password: String, email: String) {
      val collectionName = "users"
      val mongoCollection = MongoConnection()(dbName)(collectionName)
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