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

import org.bayswater.musicrest.model._
import com.mongodb.casbah.Imports._
import org.bayswater.musicrest.Util._

/** generate java script for the trad tune store web application 
    this will be deprecated once we rely of the new tunebank client */
object TuneStoreJS {

 def main(args: Array[String]): Unit = {

   if (args.length < 1) {
      println("Usage: TuneStoreJS <db name>")
      System.exit(0)
   }

   val dbName = args(0)
   val fileName = "tradtunestore.js"
   val file = new java.io.File(fileName)
   val contents = prefix + caseStatements(dbName) + suffix
   writeTextFile(file, contents.lines)
   println(fileName + " generated")
 }

 def prefix: String = {
 """var arr;
 var option;

 // dynamic selection drop-downs
 function rhythmChange(genre, includeAny) {
   var rhythm_dropdown = document.getElementById("rhythm_dropdown");
   // Set each option to null thus removing it
   while ( rhythm_dropdown.options.length ) rhythm_dropdown.options[0] = null;

   switch (genre) {
 """
 }

 def suffix: String = {
 """
    default:
      arr = new Array("")
    break;
   }

   if (includeAny) {
     arr = new Array("any").concat(arr0);
   }
   else {
     arr = arr0
   }

   for (var i=0;i<arr.length;i++) {
     option = new Option(arr[i],arr[i]);
     rhythm_dropdown.options[i] = option;
   }
   rhythm_dropdown.disabled = false;
  }

  function selectGenre(genre) {
    var element = document.getElementById('genre');
    element.value = genre;
  }

  function selectRhythm(rhythm) {
    var element = document.getElementById('rhythm_dropdown');
    element.value = rhythm;
  }

  function genreInit(genre) {
     selectGenre(genre)
     var genre_dropdown = document.getElementById("genre");
     rhythmChange(genre, true)
  }

  function genreAndRhythmInit(genre, rhythm) {
     genreInit(genre)
     selectRhythm(rhythm)
  }
 """
 }

 def caseStatements(dbName: String): String = {
   val tuneModel = new TuneModelCasbahImpl(MongoClient(), dbName)
   val sb = StringBuilder.newBuilder
   val genres = tuneModel.getSupportedGenres()
   genres.foreach( g=> {
     sb append ("\n    case \"" + g + "\":\n" )
     val rhythms = tuneModel.getSupportedRhythmsFor(g)
     val quotedRhythms = rhythms.map( r => "\"" + r + "\"")
     sb.append(quotedRhythms.mkString("      arr0 = new Array(",   ",",   ");\n"))
     sb.append ("    break;\n")
   })
   sb.mkString
 }

}
