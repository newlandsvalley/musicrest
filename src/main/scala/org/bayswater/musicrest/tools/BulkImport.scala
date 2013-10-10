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

// bulk import to Mongo via the Tune Model (Casbah-based API)
class BulkImport(val abcDir: String, val dbName: String, val genre: String) {
  
  def process  {
    println("importing into " + dbName + " genre: " + genre + " from " + abcDir)
    
    val dir = new File(abcDir)
    val fileList = dir.listFiles
    for (file <- fileList) { processFile(file) }
    createIndex(genre)
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
    val tuneModel = new TuneModelCasbahImpl(MongoConnection(), dbName)
    println ("inserting " + abc.name + " if it's new")
    abc.insertIfNew(genre)
  }
  
  private def createIndex(genre: String) {    
    val tuneModel = new TuneModelCasbahImpl(MongoConnection(), dbName)
    tuneModel.createIndex(genre)
  }
}

object BulkImport {

  def main(args: Array[String]): Unit = {
    if (args.length < 3) {      
      println("Usage: BulkImport <abc dir path> <db name> <collection (genre)>")
      System.exit(0)
    }
    
    val abcDir = args(0)
    val dbName = args(1)
    val collectionName = args(2)
    val bulkImport = BulkImport(abcDir, dbName, collectionName)
    bulkImport.process
  }
  
  def apply(abcDir: String, dbName: String, genre: String) : BulkImport = new BulkImport(abcDir, dbName, genre)
  

}
