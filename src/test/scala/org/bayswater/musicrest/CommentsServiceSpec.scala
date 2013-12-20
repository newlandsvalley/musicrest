/* Copyright (C) 2011-2013 org.bayswater
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0/
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
import StatusCodes._
import MediaTypes._
import spray.http.HttpHeaders._
import spray.routing.authentication._
import spray.routing._
import scalaz.Validation
import org.bayswater.musicrest.model.{Comment, CommentsModel}
import org.bayswater.musicrest.TestData._



class CommentsServiceSpec extends RoutingSpec with MusicRestService {
  def actorRefFactory = system
  
   val before = {
    CommentsModel().deleteAllComments("irish") 
    basicUsers
    basicGenres
    insertCommentableTunes
  }
  
  
  "CommentsService" should {
    
  
     "allow a valid user to insert a comment and return success" in {
       
       Post("/musicrest/genre/irish/tune/tune1-reel/comments", CommentTestData.commentForm(1)) ~>  Authorization(BasicHttpCredentials("administrator", "adm1n1str80r")) ~> commentsRoute ~> check  
         { 
         mediaType === MediaTypes.`text/plain`
         responseAs[String] must contain("Comment inserted user: administrator") 
         }
    }       
     
     "don't allow a valid user to insert a comment against a non-existent tune" in {
       
       Post("/musicrest/genre/irish/tune/nosuchtune/comments", CommentTestData.commentForm(1)) ~>  Authorization(BasicHttpCredentials("administrator", "adm1n1str80r")) ~> commentsRoute ~> check  
         { 
         mediaType === MediaTypes.`text/plain`
         responseAs[String] must contain("Not found genre: irish tune: nosuchtune") 
         }
    }           
     
     "allow a valid user to delete his comment and return success" in {
       
       val ts = String.valueOf(System.currentTimeMillis())
       val user = "administrator"
       
       Post("/musicrest/genre/irish/tune/tune2-reel/comments", CommentTestData.commentForm(user, 1, ts)) ~>  Authorization(BasicHttpCredentials(user, "adm1n1str80r")) ~> commentsRoute
       
       Delete(s"/musicrest/genre/irish/tune/tune2-reel/comment/administrator/${ts}") ~>  Authorization(BasicHttpCredentials(user, "adm1n1str80r")) ~> commentsRoute ~> check  
         { 
         mediaType === MediaTypes.`text/plain`
         // println("exists already")
         responseAs[String] must contain("Comment deleted user: administrator") 
         }
    }            
     
    "don't allow a normal user to delete another user's comment" in {
       
       val ts = String.valueOf(System.currentTimeMillis())
       val user = "test user"
       val userEncoded = java.net.URLEncoder.encode(user, "UTF-8")  
       val deleting_user = "untrustworthy user"
       
       Post("/musicrest/genre/irish/tune/tune3-reel/comments", CommentTestData.commentForm(user, 1, ts)) ~>  Authorization(BasicHttpCredentials(user, "passw0rd1")) ~> commentsRoute
       
       Delete(s"/musicrest/genre/irish/tune/tune3-reel/comment/${userEncoded}/${ts}") ~>  Authorization(BasicHttpCredentials(deleting_user, "password2")) ~> commentsRoute ~> check  
         {
         rejection === AuthorizationFailedRejection
         }
    }    
 
     
     "allow administrator to delete another user's comment and return success" in {
       
       val ts = String.valueOf(System.currentTimeMillis())
       val user = "test user"
       val userEncoded = java.net.URLEncoder.encode(user, "UTF-8")  
       val administrator ="administrator"
       
       Post("/musicrest/genre/irish/tune/tune4-reel/comments", CommentTestData.commentForm(user, 3, ts)) ~>  Authorization(BasicHttpCredentials(user, "adm1n1str80r")) ~> commentsRoute
       
       Delete(s"/musicrest/genre/irish/tune/tune4-reel/comment/${userEncoded}/${ts}") ~>  Authorization(BasicHttpCredentials(administrator, "adm1n1str80r")) ~> commentsRoute ~> check  
         { 
         mediaType === MediaTypes.`text/plain`
         responseAs[String] must contain(s"Comment deleted user: ${user}") 
         }
    }         
     
     "allow administrator to delete all comments and return success" in {
       
       val administrator ="administrator"       
            
       Delete("/musicrest/genre/irish/comments") ~>  Authorization(BasicHttpCredentials(administrator, "adm1n1str80r")) ~> commentsRoute ~> check  
         { 
         mediaType === MediaTypes.`text/plain`
         responseAs[String] must_==("All comments removed for genre irish") 
         }
    }              
      
   "allow a valid user to edit a comment " in {      
      val ts = String.valueOf(System.currentTimeMillis())
      val user = "test user"
      val userEncoded = java.net.URLEncoder.encode(user, "UTF-8")  
       
      Post("/musicrest/genre/irish/tune/tune5-reel/comments", CommentTestData.commentForm(user, 1, ts)) ~>  Authorization(BasicHttpCredentials(user, "passw0rd1")) ~> commentsRoute
      delay(200)
      Post("/musicrest/genre/irish/tune/tune5-reel/comments", CommentTestData.commentForm(user, 2, ts)) ~>  Authorization(BasicHttpCredentials(user, "passw0rd1")) ~> commentsRoute
      delay(200)
      
      Get(s"/musicrest/genre/irish/tune/tune5-reel/comment/${userEncoded}/${ts}") ~> addHeader("Accept", "application/json")  ~> commentsRoute ~> check {       
        responseAs[String] must contain(""""subject": "subject 2"""") 
        mediaType === MediaTypes.`application/json`
      }
       
    }           
   
    
   "don't allow a normal user to edit another user's comment" in {
       
       val ts = String.valueOf(System.currentTimeMillis()) 
       val user = "test user"
       val editing_user = "untrustworthy user"
       
       Post("/musicrest/genre/irish/tune/tune6-reel/comments", CommentTestData.commentForm(user, 1, ts)) ~>  Authorization(BasicHttpCredentials(user, "passw0rd1")) ~> commentsRoute
       delay(200)
       Post("/musicrest/genre/irish/tune/tune6-reel/comments", CommentTestData.commentForm(user, 1, ts)) ~>  Authorization(BasicHttpCredentials(editing_user, "password2")) ~> commentsRoute ~> check  
         {
         rejection === AuthorizationFailedRejection
         }
    }         
   
   "allow the administrator to edit another user's comment" in {
       
       val ts = String.valueOf(System.currentTimeMillis()) 
       val user = "test user"
       val administrator = "administrator"
       
       Post("/musicrest/genre/irish/tune/tune7-reel/comments", CommentTestData.commentForm(user, 1, ts)) ~>  Authorization(BasicHttpCredentials(user, "passw0rd1")) ~> commentsRoute
       delay(200)
       Post("/musicrest/genre/irish/tune/tune7-reel/comments", CommentTestData.commentForm(user, 2, ts)) ~>  Authorization(BasicHttpCredentials(administrator, "adm1n1str80r")) ~> commentsRoute ~> check  
         {
         mediaType === MediaTypes.`text/plain`
         responseAs[String] must contain("Comment updated user: test user") 
         }
    }            
     
     "return application/json when requested for a comment of this type" in {
       val ts = String.valueOf(System.currentTimeMillis())
       val user = "test user"     
       val userEncoded = java.net.URLEncoder.encode(user, "UTF-8")  
       
       Post("/musicrest/genre/irish/tune/tune8-reel/comments", CommentTestData.commentForm(user, 4, ts)) ~>  Authorization(BasicHttpCredentials(user, "passw0rd1")) ~> commentsRoute      
       delay(200)
       Get(s"/musicrest/genre/irish/tune/tune8-reel/comment/${userEncoded}/${ts}") ~> addHeader("Accept", "application/json")  ~> commentsRoute ~> check {
       
        responseAs[String] must contain(""""subject": "subject 4"""") 
        mediaType === MediaTypes.`application/json`
      }
    } 
    
 
     
     "return text/xml when requested for a comment of this type" in {
       val ts = String.valueOf(System.currentTimeMillis())
       val user = "test user"         
       val userEncoded = java.net.URLEncoder.encode(user, "UTF-8")  
       
       Post("/musicrest/genre/irish/tune/tune9-reel/comments", CommentTestData.commentForm(user, 5, ts)) ~>  Authorization(BasicHttpCredentials(user, "passw0rd1")) ~> commentsRoute
       delay(200)   
       Get(s"/musicrest/genre/irish/tune/tune9-reel/comment/${userEncoded}/${ts}") ~> addHeader("Accept", "text/xml")  ~> commentsRoute ~> check {
        mediaType === MediaTypes.`text/xml`
        responseAs[String] must contain("""<subject>subject 5</subject>""")
      }
    }    
     
       
     "return application/json when requested for all matching comments of this type" in {
       val ts = String.valueOf(System.currentTimeMillis())
       val tsNext = String.valueOf(System.currentTimeMillis() + 10)
       val user = "test user"    
       val administrator = "administrator" 
       
       Post("/musicrest/genre/irish/tune/tune10-reel/comments", CommentTestData.commentForm(user, 4, ts)) ~>  Authorization(BasicHttpCredentials(user, "passw0rd1")) ~> commentsRoute
       delay(200)
       Post("/musicrest/genre/irish/tune/tune10-reel/comments", CommentTestData.commentForm(administrator, 5, tsNext)) ~>  Authorization(BasicHttpCredentials(administrator, "adm1n1str80r")) ~> commentsRoute         
       delay(200)
       
       Get("/musicrest/genre/irish/tune/tune10-reel/comments") ~> addHeader("Accept", "application/json")  ~> commentsRoute ~> check {
        mediaType === MediaTypes.`application/json`            
        println(responseAs[String])
        responseAs[String] must contain(""""subject": "subject 4"""")
        responseAs[String] must contain(""""subject": "subject 5"""")
      }
    } 
     
     "return text/xml when requested for all matching comments of this type" in {
       val ts = String.valueOf(System.currentTimeMillis())
       val tsNext = String.valueOf(System.currentTimeMillis() + 10)
       val user = "test user"    
       val administrator = "administrator" 
       
       Post("/musicrest/genre/irish/tune/tune11-reel/comments", CommentTestData.commentForm(user, 4, ts)) ~>  Authorization(BasicHttpCredentials(user, "passw0rd1")) ~> commentsRoute
       delay(200)
       Post("/musicrest/genre/irish/tune/tune11-reel/comments", CommentTestData.commentForm(administrator, 5, tsNext)) ~>  Authorization(BasicHttpCredentials(administrator, "adm1n1str80r")) ~> commentsRoute         
       delay(200)
       
       Get("/musicrest/genre/irish/tune/tune11-reel/comments") ~> addHeader("Accept", "text/xml")  ~> commentsRoute ~> check {
        mediaType === MediaTypes.`text/xml`       
        // println(responseAs[String])
        responseAs[String] must contain("""<subject>subject 4</subject>""")
        responseAs[String] must contain("""<subject>subject 5</subject>""")
      }
    }         
     
    "return an empty JSON document if there are no comments" in {
      
       Get("/musicrest/genre/irish/tune/tune12-reel/comments") ~> addHeader("Accept", "application/json")  ~> commentsRoute ~> check {
        mediaType === MediaTypes.`application/json`            
        println(responseAs[String])
        responseAs[String] must contain(""""comment" :[ ]""")
      }
    }    
   
  }  
  
}

 object CommentTestData {
   def commentForm(num: Int) = FormData(List("user" -> "administrator", 
                           "timestamp" -> String.valueOf(System.currentTimeMillis()), 
                           "subject" -> s"subject ${num}",
                           "text" -> s"comment ${num} text"))
                           

   def commentForm(user: String, num: Int, ts: String) = FormData(List("user" -> user, 
                           "timestamp" -> ts, 
                           "subject" -> s"subject ${num}",
                           "text" -> s"comment ${num} text"))           
                           
           
 }
