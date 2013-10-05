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

package org.bayswater.musicrest

import org.specs2.mutable.Specification
import spray.testkit.Specs2RouteTest
import org.bayswater.musicrest.model.TuneModel
import org.bayswater.musicrest.abc._
import org.bayswater.musicrest.abc.Tune.AbcType
import org.bayswater.musicrest.cache.Cache._
import org.bayswater.musicrest.TestData._

class DataModelSpec extends RoutingSpec with MusicRestService {  
 
  def actorRefFactory = system

  val before = {basicUsers; basicGenres; insertTunes} 
  val tuneModel = TuneModel()
  
  "DataModel" should {

    "allow the insert of a new tune" in {
       val validFigForaKiss = abcFor(figForaKiss)
       
       val result = for {
         v <- validFigForaKiss
         r <- v.insertIfNew("irish")
       } yield r
       
       result.fold(e => failure("insertion failed: " + e), s => s.uri("irish") must_== ("http://localhost:8080/musicrest/genre/irish/tune/a+fig+for+a+kiss-slip+jig") )
    }  
    "bar a second insertion of a tune" in {
       val validFrahers = abcFor(frahers)
       
       val result = for {
         v <- validFrahers
         r1 <- v.insertIfNew("irish")
         r2 <- v.insertIfNew("irish")
       } yield r2
       
       result.fold(e => e must_== ("Tune: fraher's jig-jig already exists"), s =>  failure("duplicate insertion allowed: "))
    }
    "assert a tune in the database exists" in {
      val result = tuneModel.exists("irish", "noon lasses-reel")
      result must_== (true)
    }
    "assert a tune outside the database doesn't exist" in {
      val result = tuneModel.exists("irish", "does+not+exist-reel")
      result must_== (false)
    }
    "return the submitter of a tune" in {
      val result = tuneModel.getSubmitter("irish", "noon lasses-reel")
      result match {
        case Some(s) => s must_== ("administrator")
        case None => failure("No submitter found")
      }
    }
    "return the notes of a tune" in {
      val result = tuneModel.getNotes("irish", "noon lasses-reel")
      result match {
        case Some(s) => s.substring(0, 12) must_== ("E|G2 BG AcBA")
        case None => failure("No notes found")
      }
    }
    "return a complete tune" in {
      val result = tuneModel.getNotes("irish", "noon lasses-reel")
      result match {
        case Some(s) => s.substring(0, 12) must_== ("E|G2 BG AcBA")
        case None => failure("No notes found")
      }
    }
    "return a list of tunes" in {      
      val genre = "irish"
      val tunes = tuneModel.getTunes(genre, "alpha", 1, 10)
      val t = tunes.next
      val optId = t.get("_id")
      optId match {
        case Some(id) => {
          val optTune = tuneModel.getTune(genre, id)
          optTune match  {
            case Some(tune) => {tune.titles(0) must_== "A Fig For A Kiss"
                                tune.kvs("_id") must_== "a fig for a kiss-slip jig"
                               }
            case None => failure(s"No tune found against listed id ${id}")
          }
        }
        case None => failure("No id found in first tune in list")
      }
    }
    "search for tunes" in {      
      val genre = "irish"
      val reels = Map("R" -> "reel")
      val tunes = tuneModel.search(genre, reels, "alpha", 1, 10)
      val t = tunes.next
      val optId = t.get("_id")
      optId match {
        case Some(id) => {
          val optTune = tuneModel.getTune(genre, id)
          optTune match  {
            case Some(tune) => {tune.titles(0) must_== "Noon Lasses"              
                                tune.kvs("_id") must_== "noon lasses-reel"
                               }
            case None => failure(s"No tune found against listed id ${id}")
          }
        }
        case None => failure("No id found in first tune in list")
      }
    }
    "count the reels" in {
      val reels = Map("R" -> "reel")
      val count = tuneModel.count("irish", reels)
      count must_== (2)
    }
  }
  
  def insertTunes = {
    val dbName = "tunedbtest"
    val tuneModel = TuneModel()
    tuneModel.delete("irish")
    val validNoonLasses = abcFor(noonLasses)
    validNoonLasses.fold(e => println("unexpected error in test data: " + e), s => tuneModel.insert("irish", s))  
    val validSpeedThePlough = abcFor(speedThePlough)
    validSpeedThePlough.fold(e => println("unexpected error in test data: " + e), s => tuneModel.insert("irish", s))  
  }

}