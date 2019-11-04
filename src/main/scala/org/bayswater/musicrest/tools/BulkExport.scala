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

/* bulk export from Mongo via the Tune Model (Casbah-based API)
 * with mongo connections defined by the musicrest configuration file
 * assumed to be loaded via a java -D parameter
 */
case class TuneRef(id: String, abc: String)

class BulkExport (val genre: String, val abcDir: String) {

  val settings = MusicRestSettings

  val mongoClient = MongoCasbahUtil.buildMongoClient(settings.dbHost, settings.dbPort, settings.dbLogin, settings.dbPassword, settings.dbName)

  def process  {
    println("exporting to abcdir: " + abcDir + " from host: " + settings.dbHost + " database: " + settings.dbName + " genre: " + genre)
    val pageSize = 20
    var nextPage = 1
    var exported = 0
    val tuneModel = new TuneModelCasbahImpl(mongoClient, settings.dbName)
    def count = tuneModel.count(genre, Map.empty[String, String]) : Long
    while (exported < count ) {
       val tunes: Iterator[scala.collection.Map[String, String]] = tuneModel.getTunes(genre, "alphabetic", nextPage, pageSize)
       writeTunes(tunes, abcDir, genre, settings.dbName)
       exported += pageSize
       nextPage += 1
    }
   }

  def writeTunes(tunes: Iterator[scala.collection.Map[String, String]], abcDir: String, genre: String, dbName: String) {
    val tuneModel = new TuneModelCasbahImpl(mongoClient, dbName)
    tunes.foreach(t => {
      val tuneOpt =  for {
        id <- t.get(TuneModel.tuneKey)
        tune <- tuneModel.getTune(genre, id)
      } yield ( tune )
      tuneOpt match {
        case Some(t) => {
          val abc = Abc(t)
          writeTune(abc, abcDir)
        }
        case None => println("not found")
      }
    })
  }

  def writeTune(abc: Abc, abcDir: String) {
    println("exporting abc for " + abc.safeFileName)
    val contents = abc.abc
    val dir = new File(abcDir)
    val abcFile = new File(dir, abc.safeFileName + ".abc")
    Util.writeTextFile(abcFile, contents.lines)
  }
}

object BulkExport {

  def main(args: Array[String]): Unit = {
    if (args.length < 2) {
      println("Usage: BulkExport  <collection (genre)> <abc dir path>")
      System.exit(0)
    }

    val collectionName = args(0)
    val abcDir = args(1)
    val bulkExport = BulkExport(collectionName, abcDir)
    bulkExport.process
  }

  def apply(genre: String, abcDir: String) : BulkExport =
    new BulkExport(genre, abcDir)
}
