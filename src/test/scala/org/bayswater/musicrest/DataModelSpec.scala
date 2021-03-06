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

/** Note, some of these tests that involve multiple insert/replace attempts of the same tune require delays.
 *  This is so as not only to emulate actual conditions but to take account of Mongo's seeming asynchronous nature.
 * 
 */
class DataModelSpec extends RoutingSpec with MusicRestService {  
 
  def actorRefFactory = system

  val before = {basicUsers; basicGenres; clearIrishTunes; insertTunes} 
  val tuneModel = TuneModel()
  
  "DataModel" should {
    
    "allow the insert of a new tune" in {
       val validFigForaKiss = abcFor("irish", figForaKiss)
       
       val result = for {
         v <- validFigForaKiss.disjunction
         r <- v.upsert("irish").disjunction
       } yield r
       
       result.fold(e => validationFailure("insertion failed: " + e), s => s.uri("irish") must_== ("http://localhost:8080/musicrest/genre/irish/tune/a+fig+for+a+kiss-slip+jig") )
    }  
    "bar a replacement of a tune by a different user" in {
       val validFrahersU1 = abcFor("irish", frahers, "test user")
       val validFrahersU2 = abcFor("irish", frahers, "untrustworthy user")
       
       val result = for {
         v1 <- validFrahersU1.disjunction
         v2 <- validFrahersU2.disjunction
         r1 <- v1.upsert("irish").disjunction
         _ <- delay(4000).disjunction
         r2 <- v2.upsert("irish").disjunction
       } yield r2
       
       result.fold(e => e must_== ("Tune: fraher's jig-jig already exists - original submitter: test user"), 
                        s =>  validationFailure("duplicate insertion allowed: "))
    }  
    "allow a replacement of a tune by the original user" in {
       val validTempest = abcFor("irish", tempest, "test user")
       
       val result = for {
         v <- validTempest.disjunction
         r1 <- v.upsert("irish").disjunction
         _ <- delay(3000).disjunction
         r2 <- v.upsert("irish").disjunction
       } yield r2
       
       result.fold(e => validationFailure("replacement by same user barred: " + e), 
                   s => s.uri("irish") must_== ("http://localhost:8080/musicrest/genre/irish/tune/tempest-reel"))
    }  
    "ensure index is unique in" in {
       val validFrahers = abcFor("irish", noonLasses)
       
       val result = for {
         v <- validFrahers.disjunction
         r <- tuneModel.insert("irish", v).disjunction
       } yield r
       
       result.fold(e => e must contain ("duplicate key error"), s =>  validationFailure("duplicate insertion allowed: "))
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
        case None => validationFailure("No submitter found")
      }
    }
    "return the notes of a tune" in {
      val result = tuneModel.getNotes("irish", "noon lasses-reel")
      result match {
        case Some(s) => s.substring(0, 12) must_== ("E|G2 BG AcBA")
        case None => validationFailure("No notes found")
      }
    }
    "return the GUID of a tune" in {
      val result = tuneModel.getTuneRef("irish", "noon lasses-reel")
      val now = new java.util.Date()
      result match {
        case Some(s) =>  {
          // rudimentary validation for a valid Mongo ObjectId
          val earlier = new java.util.Date(s.getTime())
          now.after(earlier) must_== (true)
        }
        case None => validationFailure("No tune GUID found")
      }
    }
    "allow the replacement of an existing tune" in {
      val result = tuneModel.getTuneRef("irish", "noon lasses-reel")
      result match {
        case Some(ref) =>  {
          // this tune already should exist in the db but we'll discriminate the update by adding a new title
          val validNoonLasses = abcFor("irish", noonLasses)
       
          val result = for {
            abc1 <- validNoonLasses.disjunction
            abc2 <- abc1.addAlternativeTitle("another title").disjunction
            r <- TuneModel().replace(ref, "irish", abc2).disjunction
          } yield r 
          
          result.fold(e => validationFailure("replacement failed: " + e), s => s must_== ("Tune Noon Lasses replaced in irish") )          
          
          val headersOpt = tuneModel.getAbcHeaders("irish", "noon lasses-reel")
          headersOpt match {
            case Some(hs) => hs must contain ("another title")
            case None => validationFailure("No tune headers read back after replacement")
          }
        }
        case None => validationFailure("Test data error - No original tune GUID found")
      }      
    }  
    "return a complete tune" in {
      val result = tuneModel.getNotes("irish", "noon lasses-reel")
      result match {
        case Some(s) => s.substring(0, 12) must_== ("E|G2 BG AcBA")
        case None => validationFailure("No notes found")
      }
    }
    "return a list of tunes" in {      
      val genre = "irish"

      // ensure we have a fig for a kiss in the db (which sorts first)
      val validFigForaKiss = abcFor("irish", figForaKiss)       

      try {
      	   val result = for {
           v <- validFigForaKiss.disjunction
           r <- tuneModel.insert(genre, v).disjunction
           } yield r 
      }
      catch {
         case e:Throwable => 
      }
       
      val tunes = tuneModel.getTunes(genre, "alpha", 1, 10)
      val t = tunes.next
      val optId = t.get(TuneModel.tuneKey)
      optId match {
        case Some(id) => {
          val optTune = tuneModel.getTune(genre, id)
          optTune match  {
            case Some(tune) => {tune.titles(0) must_== "A Fig For A Kiss"
                                tune.kvs(TuneModel.tuneKey) must_== "a fig for a kiss-slip jig"
                               }
            case None => validationFailure(s"No tune found against listed id ${id}")
          }
        }
        case None => validationFailure("No id found in first tune in list")
      }
    }
    "search for tunes" in {      
      val genre = "irish"
      val reels = Map("R" -> "reel")
      val tunes = tuneModel.search(genre, reels, "alpha", 1, 10)
      val t = tunes.next
      val optId = t.get(TuneModel.tuneKey)
      optId match {
        case Some(id) => {
          val optTune = tuneModel.getTune(genre, id)
          optTune match  {
            case Some(tune) => {tune.titles(0) must_== "Noon Lasses"              
                                tune.kvs(TuneModel.tuneKey) must_== "noon lasses-reel"
                               }
            case None => validationFailure(s"No tune found against listed id ${id}")
          }
        }
        case None => validationFailure("No id found in first tune in list")
      }
    }
    "count the reels" in {
      val reels = Map("R" -> "reel")
      val count = tuneModel.count("irish", reels)
      count must_== (3)
    }   
    "add an alternative title in" in {
      val genre = "irish"
      val id = "speed the plough-reel"
      val newTitle = "Cronins"
      val result = tuneModel.addAlternativeTitle(genre, id, newTitle)
      // we now just return a reference to the tune if it succeeds
      result.fold(e => validationFailure("update failed: " + e), s => s must_== (s"Tune title ${newTitle} added") )    
      val headersOption =  tuneModel.getAbcHeaders(genre, id)
      headersOption match {
        case None => validationFailure("No ABC headers after update")
        case Some(h) => {
          h must contain("Speed The Plough")
          h must contain("Cronins")
          }
        }
      }  
    
  }
  
  def insertTunes = {
    println("insertTunes")
    val dbName = "tunedbtest"
    val tuneModel = TuneModel()
    val genre = "irish"
    tuneModel.delete(genre)
    val validNoonLasses = abcFor(genre, noonLasses)
    validNoonLasses.fold(e => println("unexpected error in test data: " + e), s => s.upsert("irish"))  
    val validSpeedThePlough = abcFor(genre, speedThePlough)
    validSpeedThePlough.fold(e => println("unexpected error in test data: " + e), s => s.upsert("irish"))  
    tuneModel.createIndex("irish")
  }  

  // this is a hack to get the types to match up - must find out how to do it properly
  def validationFailure (msg: String) =
     msg must_== "true"

}
