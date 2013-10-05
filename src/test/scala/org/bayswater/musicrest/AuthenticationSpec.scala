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
import spray.testkit._
import spray.http._
import spray.http.HttpHeaders._
import StatusCodes._
import spray.routing._
import AuthenticationFailedRejection._
import org.bayswater.musicrest.model.{TuneModel,User}
import org.bayswater.musicrest.TestData._

class AuthenticationSpec extends RoutingSpec with MusicRestService {
  def actorRefFactory = system
  
  val before = {basicUsers; basicGenres; insertTunesForDeletion} 
  
  /* expected response challenge header */
  val challenge = `WWW-Authenticate`(HttpChallenge("Basic", "musicrest"))
  
    
  "The MusicRestService" should {
    "request authentication parameters for posted tunes" in {  
       Post("/musicrest/genre/irish/tune") ~> musicRestRoute ~> check 
         { rejection === AuthenticationFailedRejection(CredentialsMissing, List(challenge)) }
    }  
    "request authentication parameters for test-transcode posted tunes" in {  
       Post("/musicrest/genre/irish/transcode") ~> musicRestRoute ~> check 
         { rejection === AuthenticationFailedRejection(CredentialsMissing, List(challenge)) }
    }
    "reject authentication for unknown credentials" in {
       Post("/musicrest/genre/irish/tune") ~>  Authorization(BasicHttpCredentials("foo", "bar")) ~> musicRestRoute ~> check 
         { rejection === AuthenticationFailedRejection(CredentialsRejected, List(challenge)) }
    }
    "reject authentication for known credentials  but as yet unvalidated users" in {
       Post("/musicrest/genre/irish/tune") ~>  Authorization(BasicHttpCredentials("mathias", "math1as")) ~> musicRestRoute ~> check 
         { rejection === AuthenticationFailedRejection(CredentialsRejected, List(challenge)) }
    }
    "allow authentication for known credentials (but reject the lack of a POST body as a bad request)" in {
       Post("/musicrest/genre/irish/tune") ~>  Authorization(BasicHttpCredentials("test user", "passw0rd1")) ~> musicRestRoute ~> check 
         { rejection === RequestEntityExpectedRejection }
    }
    "don't allow normal users to delete tunes" in {
       Delete("/musicrest/genre/irish/tune/sometune") ~>  Authorization(BasicHttpCredentials("untrustworthy user", "passw0rd2")) ~> musicRestRoute ~> check 
         { rejection === AuthenticationFailedRejection(CredentialsRejected, List(challenge)) }
    }
    "don't allow normal users to view the user list" in {
       Get("/musicrest/user") ~>  Authorization(BasicHttpCredentials("test user", "passw0rd1")) ~> musicRestRoute ~> check 
         { rejection === AuthenticationFailedRejection(CredentialsRejected, List(challenge)) }
    }
    "allow administrators to delete tunes" in {
       Delete("/musicrest/genre/irish/tune/noon+lasses-reel") ~>  Authorization(BasicHttpCredentials("administrator", "adm1n1str80r")) ~> musicRestRoute ~> check 
         { entityAs[String] must contain("Tune noon lasses-reel removed from irish") }
    } 
    "allow original submitter to delete tunes" in {
       Delete("/musicrest/genre/irish/tune/speed+the+plough-reel") ~>  Authorization(BasicHttpCredentials("test user", "passw0rd1")) ~> musicRestRoute ~> check 
         { entityAs[String] must contain("Tune speed the plough-reel removed from irish") }
    } 
    "reject authentication for unknown credentials in user check" in {
       Get("/musicrest/user/check") ~>  Authorization(BasicHttpCredentials("foo", "bar")) ~> musicRestRoute ~> check 
         { rejection === AuthenticationFailedRejection(CredentialsRejected, List(challenge)) }
    }
    "allow authentication for known credentials in user check" in {
       Get("/musicrest/user/check") ~>  Authorization(BasicHttpCredentials("test user", "passw0rd1")) ~> musicRestRoute ~> check 
         { entityAs[String] must contain("user is valid") }
    }
  }
  
  
  
   /** two different tunes submitted by test user */
   def insertTunesForDeletion = {
     val dbName = "tunedbtest"
     val tuneModel = TuneModel()
     tuneModel.delete("irish")
     val validNoonLasses = abcFor(noonLasses,"test user")
     validNoonLasses.fold(e => println("unexpected error in test data: " + e), s => s.insertIfNew("irish"))  
     val validSpeedThePlough = abcFor(speedThePlough,"test user")
     validSpeedThePlough.fold(e => println("unexpected error in test data: " + e), s => s.insertIfNew("irish"))  
  }  
  
}
