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
import spray.http._
import spray.http.HttpHeaders._
import StatusCodes._
import MediaTypes._
import org.bayswater.musicrest.model.TuneModel
import org.bayswater.musicrest.abc._
import org.bayswater.musicrest.abc.Tune.AbcType
import org.bayswater.musicrest.cache.Cache._
import org.bayswater.musicrest.TestData._



/* These tests exercise URLs which require content negotiation */
class ContentNegotiationSpec extends RoutingSpec with MusicRestService {
  def actorRefFactory = system

  val before = {basicUsers; basicGenres; insertTunes; clearTuneCache}

  "MusicRestService" should {

    /* Tunes */
    "return text/plain content when requested for tunes of this type" in {
      Get("/musicrest/genre/irish/tune/noon+lasses-reel") ~> addHeader("Accept", "text/plain")  ~> musicRestRoute ~> check {
        mediaType === MediaTypes.`text/plain`
        responseAs[String] must contain("T: Noon Lasses")
        responseAs[String] must contain("M: 4/4")
      }
    }
    
    "return text/vnd.abc content when requested for tunes of this type" in {
      Get("/musicrest/genre/irish/tune/noon+lasses-reel") ~> addHeader("Accept", "text/vnd.abc")  ~> musicRestRoute ~> check {
        mediaType === AbcType        
        responseAs[String] must contain("T: Noon Lasses")
        responseAs[String] must contain("M: 4/4")
      }
    }

   "return text/xml content when requested for tunes of this type" in {
      Get("/musicrest/genre/irish/tune/noon+lasses-reel") ~> addHeader("Accept", "text/xml")  ~> musicRestRoute ~> check {
        mediaType === MediaTypes.`text/xml`
        responseAs[String] must contain("<T>Noon Lasses</T>")
      }
    }   

   "return application/json content when requested for tunes of this type" in {
      Get("/musicrest/genre/irish/tune/speed+the+plough-reel") ~> addHeader("Accept", "application/json")  ~> musicRestRoute ~> check {
        mediaType === MediaTypes.`application/json`
        responseAs[String] must contain("""T": "Speed The Plough""")
      }
    }

   "return application/pdf content when requested for tunes of this type" in {
      Get("/musicrest/genre/irish/tune/noon+lasses-reel") ~> addHeader("Accept", "application/pdf")  ~> musicRestRoute ~> check {
        mediaType === MediaTypes.`application/pdf`
      }
    }

   "return audio/midi content when requested for tunes of this type" in {
      Get("/musicrest/genre/irish/tune/noon+lasses-reel") ~> addHeader("Accept", "audio/midi")  ~> musicRestRoute ~> check {
        mediaType === MediaTypes.`audio/midi`
      }
    }

   "return image/png content when requested for tunes of this type" in {
      Get("/musicrest/genre/irish/tune/noon+lasses-reel") ~> addHeader("Accept", "image/png")  ~> musicRestRoute ~> check {
        mediaType === MediaTypes.`image/png`
      }
    }   

    /* Note, as of this version of Spray, this will raise a Spray exception which is caught by Spray */
   /*
    "return text/plain content when the tune doesn't exist" in {
      Get("/musicrest/genre/irish/tune/no-such-tune") ~> addHeader("Accept", "text/plain")  ~> musicRestRoute ~> check {
        mediaType === MediaTypes.`text/plain`
      }
    }
    * 
    */
    
   /* Genre Rhythm Lists */
   "return text/xml content when requested for a genre list of this type" in {
      Get("/musicrest/genre/scandi") ~> addHeader("Accept", "text/xml")  ~> sealRoute(musicRestRoute) ~> check {
        mediaType === MediaTypes.`text/xml`
        responseAs[String] must contain("<rhythm>polska</rhythm>")
      }
    }      
   
    "return application/json content when requested for a genre list of this type" in {
      Get("/musicrest/genre/scandi") ~> addHeader("Accept", "application/json")  ~> musicRestRoute ~> check {
        mediaType === MediaTypes.`application/json`
        responseAs[String] must contain("""{ "rhythm" :[""")
      }
    }  
    
  "return text/html content when requested for a genre list of this type" in {
      Get("/musicrest/genre/scandi") ~> addHeader("Accept", "text/html")  ~> musicRestRoute ~> check {
        mediaType === MediaTypes.`text/html`
        responseAs[String] must contain("<p>polska")
      }
    }          
  
   /* Genre Lists */
   "return text/xml content when requested for a genre list of this type" in {
      Get("/musicrest/genre") ~> addHeader("Accept", "text/xml")  ~> musicRestRoute ~> check {
        mediaType === MediaTypes.`text/xml`
        responseAs[String] must contain("<genre>scandi</genre>")
      }
    }      
   
    "return application/json content when requested for a genre list of this type" in {
      Get("/musicrest/genre") ~> addHeader("Accept", "application/json")  ~> musicRestRoute ~> check {
        mediaType === MediaTypes.`application/json`
        responseAs[String] must contain("""["scottish","scandi","irish","klezmer"]""")
      }
    }  
    
  "return text/html content when requested for a genre list of this type" in {
      Get("/musicrest/genre") ~> addHeader("Accept", "text/html")  ~> musicRestRoute ~> check {
        mediaType === MediaTypes.`text/html`
        responseAs[String] must contain("<p>scandi")
      }
    }          
  
   /* Tune Lists */
   "return text/xml content when requested for a tune list of this type" in {
      Get("/musicrest/genre/irish/tune") ~> addHeader("Accept", "text/xml")  ~> musicRestRoute ~> check {
        mediaType === MediaTypes.`text/xml`
        responseAs[String] must contain("<tunes>")
        responseAs[String] must contain("""<uri>noon+lasses-reel</uri>""")
        responseAs[String] must contain("""<pagination><page>1</page><size>10</size></pagination>""")
        checkPaginationHeader(header("Musicrest-Pagination"))
      }
    }      
   
    "return application/json content when requested for a tune list of this type" in {
      Get("/musicrest/genre/irish/tune") ~> addHeader("Accept", "application/json")  ~> musicRestRoute ~> check {
        mediaType === MediaTypes.`application/json`
        responseAs[String] must contain(""""uri": "noon+lasses-reel"""")
        responseAs[String] must contain(""" "pagination" : """)
        checkPaginationHeader(header("Musicrest-Pagination"))
      }
    }  
    
  "return text/html content when requested for a tune list of this type" in {
      Get("/musicrest/genre/irish/tune") ~> addHeader("Accept", "text/html")  ~> musicRestRoute ~> check {
        mediaType === MediaTypes.`text/html`
        responseAs[String] must contain("""<td><a href="genre/irish/tune/noon+lasses-reel" >noon lasses</a>""")
        responseAs[String] must contain("""<span class="tunelist" page="1" size="10" >""")
        checkPaginationHeader(header("Musicrest-Pagination"))
      }
    }    
  
    /* User Lists */
   "return text/xml content when requested for a user list of this type" in {
      Get("/musicrest/user") ~> addHeader("Accept", "text/xml") ~> Authorization(BasicHttpCredentials("administrator", "adm1n1str80r")) ~> musicRestRoute ~> check {
        mediaType === MediaTypes.`text/xml`
        responseAs[String] must contain("<name>administrator</name>")
        checkPaginationHeader(header("Musicrest-Pagination"))
      }
    }   
   
    "return application/json content when requested for a user list of this type" in {
      Get("/musicrest/user") ~> addHeader("Accept", "application/json")  ~> Authorization(BasicHttpCredentials("administrator", "adm1n1str80r")) ~> musicRestRoute ~> check {
        mediaType === MediaTypes.`application/json`
        responseAs[String] must contain(""""name": "administrator" """)
        checkPaginationHeader(header("Musicrest-Pagination"))
      }
    }  
    
   
  "return text/html content when requested for a user list of this type" in {
      Get("/musicrest/user") ~> addHeader("Accept", "text/html")  ~> Authorization(BasicHttpCredentials("administrator", "adm1n1str80r")) ~> musicRestRoute ~> check {
        mediaType === MediaTypes.`text/html`
        responseAs[String] must contain("<td>administrator</td>")
        checkPaginationHeader(header("Musicrest-Pagination"))
      }
    } 

  }
  
  def checkPaginationHeader(h: Option[spray.http.HttpHeader]): Boolean = h match {
    case None => failure("No pagination header")
    case Some(h) => h.value must contain ("1 of 1")
  }  

  

  def insertTunes = {
   val dbName = "tunedbtest"
   val tuneModel = TuneModel()
   val genre = "irish"
   tuneModel.delete(genre)
   val validNoonLasses = abcFor(genre, noonLasses)
   validNoonLasses.fold(e => println("unexpected error in test data: " + e), s => s.upsert("irish"))  
   val validSpeedThePlough = abcFor(genre, speedThePlough)
   validSpeedThePlough.fold(e => println("unexpected error in test data: " + e), s => s.upsert("irish"))  
  }
  
  def clearTuneCache = {
    val cacheDir = new java.io.File("cache/test")
    clearCache(cacheDir, 0)
  }
  
  
}




