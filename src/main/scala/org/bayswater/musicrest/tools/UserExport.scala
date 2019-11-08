/*
 * Copyright (C) 2011-2019 org.bayswater
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

import java.io.{File, FileInputStream, BufferedInputStream}
import scalaz.Validation
import com.mongodb.casbah.Imports._
import org.bayswater.musicrest.MusicRestSettings
import org.bayswater.musicrest.Util
import org.bayswater.musicrest.abc._
import org.bayswater.musicrest.model._
import org.bayswater.musicrest.Util._

/* bulk users export from Mongo via the User Model (Casbah-based API)
 * with mongo connections defined by the musicrest configuration file
 * assumed to be loaded via a java -D parameter
 */
class UserExport (val dir: String) {

  val settings = MusicRestSettings

  val mongoClient = MongoCasbahUtil.buildMongoClient(settings.dbHost, settings.dbPort, settings.dbLogin, settings.dbPassword, settings.dbName)

  def process  {
    println("exporting users to dir: " + dir + " from host: " + settings.dbHost + " database: " + settings.dbName)
    val pageSize = 100
    var nextPage = 1
    var exported = 0
    val userModel = new UserModelCasbahImpl(mongoClient, settings.dbName)
    def count = userModel.userCount() : Long
    println("number of users: " + count)
    while (exported < count ) {
       val users: Iterator[UserRef] = userModel.getUsers(nextPage, pageSize)
       writeUsers(users, dir, nextPage)
       exported += pageSize
       nextPage += 1
    }
   }

  def writeUsers(userIter: Iterator[UserRef], dirName: String, pageNo : Int) {
    val userStrings = userIter.map(toString)
    writeUserFile(userStrings, dirName, pageNo)
  }

  def toString (u : UserRef) : String = {
    println("exporting user: " + u.name)
    u.name + " " + u.password + " " + u.email + " " + u.valid + "\r\n"
  }

  def writeUserFile(users: Iterator[String], dirName: String, pageNo: Int) {
    val dir = new File(dirName)
    val fileName = "users" + pageNo.toString + ".txt"
    val usersFile = new File(dir, fileName)
    Util.writeTextFile(usersFile, users)
  }
}

object UserExport {

  def main(args: Array[String]): Unit = {
    if (args.length < 1) {
      println("Usage: UserExport  <dir path>")
      System.exit(0)
    }

    val dirName = args(0)
    val userExport = UserExport(dirName)
    userExport.process
  }

  def apply(dirName: String) : UserExport =
    new UserExport(dirName)
}
