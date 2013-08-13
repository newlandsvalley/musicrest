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

import java.io.{File, FileInputStream, BufferedInputStream}
import scalaz.Validation
import com.mongodb.casbah.Imports._
import org.bayswater.musicrest.Util
import org.bayswater.musicrest.abc._
import org.bayswater.musicrest.model._
import org.bayswater.musicrest.Util._

case class TuneRef(id: String, abc: String)

class BulkExport (val abcDir: String, val genre: String, dbName: String) {
  def process  {
    println("abcdir: " + abcDir)
    val pageSize = 20
    var nextPage = 1
    var exported = 0
    val tuneModel = new TuneModelCasbahImpl(MongoConnection(), dbName)
    def count = tuneModel.count(genre, Map.empty[String, String]) : Long
    while (exported < count ) {
       val tunes: Iterator[scala.collection.Map[String, String]] = tuneModel.getTunes(genre, "alphabetic", nextPage, pageSize)
       writeTunes(tunes, abcDir, genre, dbName)
       exported += pageSize
       nextPage += 1
    }
   }
  
  def writeTunes(tunes: Iterator[scala.collection.Map[String, String]], abcDir: String, genre: String, dbName: String) {    
    val tuneModel = new TuneModelCasbahImpl(MongoConnection(), dbName)
    tunes.foreach(t => {
      val tuneOpt =  for {
        id <- t.get("_id")
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
    if (args.length < 3) {      
      println("Usage: BulkExport <abc dir path> <db name> <genre name>")
      System.exit(0)
    }
    
    val abcDir = args(0)
    val dbName = args(1)
    val genre = args(2)
    val bulkExport = BulkExport(abcDir, genre, dbName)
    bulkExport.process
  }
  
  def apply(abcDir: String, genre: String, dbName: String) : BulkExport = new BulkExport(abcDir, genre, dbName)  

}