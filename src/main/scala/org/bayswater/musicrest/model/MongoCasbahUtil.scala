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

import com.mongodb.casbah.Imports._
import com.mongodb.ServerAddress

/* Utility to get a Mongo Casbah client connection */

object MongoCasbahUtil {

  def buildMongoClient (dbHost: String, dbPort: Int, dbLogin: String, dbPassword: String, dbName: String, poolSize: Option[Int]= None) : MongoClient  = {
    val password = dbPassword.toCharArray
    val server = new ServerAddress(dbHost,  dbPort)
    val credentials = MongoCredential.createCredential(dbLogin, dbName, password)
    poolSize match {
      case Some(size) =>
        val mongoOptions = MongoClientOptions ( connectionsPerHost = size )
        MongoClient(server, List(credentials), mongoOptions)
      case None =>
        MongoClient(server, List(credentials))
    }
  }
}
