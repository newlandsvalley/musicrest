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

package org.bayswater.musicrest.abc


import spray.util.LoggingContext  
import org.bayswater.musicrest.model.TuneModel
import org.bayswater.musicrest.MusicRestSettings

object SupportedGenres {
  
  lazy val genres: List[String] = TuneModel().getSupportedGenres() 
  val musicRestSettings = MusicRestSettings
  
  val rhythmMap: Map[String, List[String]] = {
    
    val builder = Map.newBuilder[String, List[String]]
      
    genres.map(genre => {
        val rhythmList = TuneModel().getSupportedRhythmsFor(genre)
        builder += ((genre, rhythmList))      
      }
    )
      
    builder.result
  }
  
  def isGenre(genre: String): Boolean = {
    genres.contains(genre.toLowerCase())
  }
  
  def isRhythm(genre: String, rhythm: String): Boolean = {
    val rhythms: List[String] = rhythmMap.getOrElse(genre, List.empty)
    rhythms.contains(rhythm)
  }  

  def rhythms(genre: String): List[String] = 
    rhythmMap.getOrElse(genre, List(genre + " is unknown: no rhythms established"))
  
  def createGenreSubdirectories()(implicit log: LoggingContext) : Unit = {
    // genre specific directories
    createDirIfNew(musicRestSettings.dirCore)
    createDirIfNew(musicRestSettings.dirTry)
    createDirIfNew(musicRestSettings.dirPlay)
    genres.foreach(g =>  { 
      createSubdir(musicRestSettings.abcDirCore, g)
      createSubdir(musicRestSettings.pdfDirCore, g) 
      createSubdir(musicRestSettings.abcDirTry, g)
      createSubdir(musicRestSettings.pdfDirTry, g) 
      createSubdir(musicRestSettings.abcDirPlay, g)
      createSubdir(musicRestSettings.wavDirPlay, g)      
    })    
  }  
  
  private def createSubdir(parentPath: String, genre: String)(implicit log: LoggingContext) {
    createDirIfNew(parentPath)
    createDirIfNew(parentPath + "/" + genre)
  }

  private def createDirIfNew(path: String)(implicit log: LoggingContext) {
    val dir: java.io.File = new java.io.File(path)
    if (!dir.exists()) {
      val ok = dir.mkdir()
      log.info(s"making cache directory ${dir.getPath()} result $ok")
    }
  }

}
