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

/* bulk import to Mongo via the Tune Model (Casbah-based API)
 * with mongo connections defined by the musicrest configuration file
 * assumed to be loaded via a java -D parameter
 */
class BulkImport(val genre: String, val abcDir: String) {

  val settings = MusicRestSettings

  def process  {
    println("importing from abcdir: " + abcDir + " on host: " + settings.dbHost + " database: " + settings.dbName + " genre: " + genre)

    val dir = new File(abcDir)
    val fileList = dir.listFiles
    for (file <- fileList) { processFile(file) }
   }

  private def processFile(file: File) {
    println(file.getName())
    val bis = new BufferedInputStream(new FileInputStream(file))
    val contents = new String(Util.readInput(bis))
    val abcSubmission = AbcSubmission(contents.lines, genre, "administrator")
    val validAbc:Validation[String, Abc] = abcSubmission.validate
    validAbc.fold(e => println("unexpected error: " + e), s => insertToMongo(s))
  }

  private def insertToMongo(abc: Abc) {
    println ("inserting " + abc.name + " if it's new")
    abc.insertIfNew(genre)
  }
}

object BulkImport {

  def main(args: Array[String]): Unit = {
    if (args.length < 2) {
      println("Usage: BulkImport  <collection (genre)> <abc dir path>")
      System.exit(0)
    }

    val collectionName = args(0)
    val abcDir = args(1)

    val bulkImport = BulkImport(collectionName, abcDir)
    bulkImport.process
  }

  def apply(genre: String, abcDir: String) : BulkImport =
    new BulkImport(genre, abcDir)

}
