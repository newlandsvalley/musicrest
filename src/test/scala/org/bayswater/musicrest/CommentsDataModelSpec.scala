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
import org.bayswater.musicrest.model.{CommentsModel, Comment}
import org.bayswater.musicrest.TestData._


class CommentsDataModelSpec extends Specification {  
  
  val commentsModel = CommentsModel()  
 
  val before = {
    CommentsModel().deleteAllComments("irish") 
    basicUsers
    basicGenres
    insertCommentableTunes
  }
  
  "CommentsModel" should {
    
    "allow the addition of a comment" in {
       val ts = String.valueOf(System.currentTimeMillis())
       val comment = Comment("john", ts, "initial comment", "initial comment text")
       
       val result = commentsModel.insertComment("irish", "tune1-reel", comment)
       
       result.fold(e => failure("insertion failed: " + e), s => s must_== (s"Comment inserted user: john id: ${ts} for tune tune1-reel") )
    }     
    
    "allow the update of a comment" in {
       val ts = String.valueOf(System.currentTimeMillis())
       val comment1 = Comment("john", ts, "initial comment", "initial comment text")
       
       commentsModel.insertComment("irish", "tune12-reel", comment1)
       delay(200)
       val comment2 = Comment("john", ts, "updated comment", "updated comment text")
       
       val result = commentsModel.insertComment("irish", "tune12-reel", comment2)
       
       result.fold(e => failure("insertion failed: " + e), s => s must_== (s"Comment updated user: john id: ${ts} for tune tune12-reel") )
    }   
    
    "retrieve a comment" in {
       val ts = String.valueOf(System.currentTimeMillis())
       val comment = Comment("john", ts, "initial comment", "initial comment text")
       
       commentsModel.insertComment("irish", "tune2-reel", comment)
       val result = commentsModel.getComment("irish", "tune2-reel", "john", ts)
       
       result.fold(e => failure("retrieval failed: " + e), s => s must_== comment )
    }        
    
   "get a set of comments" in {
       val ts1 = String.valueOf(System.currentTimeMillis())
       val comment1 = Comment("john", ts1, "comment1", "comment 1 text")
       val ts2 = String.valueOf(System.currentTimeMillis() + 1)
       val comment2 = Comment("john", ts2, "comment2", "comment 2 text")
       
       val genre = "irish"
       val tune = "tune3-reel"
       
       commentsModel.insertComment(genre, tune, comment1)
       commentsModel.insertComment(genre, tune, comment2)
       
       val comments = commentsModel.getComments(genre, tune)
       
       comments.size must_== (2)
    }      
    
    "raise an appropriate error when retrieving a non-existent comment" in {
       val ts = String.valueOf(-1L)
       val result = commentsModel.getComment("irish", "tune3-reel", "john", ts)       
       result.fold(e => e must_== "Comment not found for tune tune3-reel and user john and id -1", s => failure("Retrieved non-existent comment") )
    }       
    
    "allow update of comments" in {      
       val ts = String.valueOf(System.currentTimeMillis())
       val comment1 = Comment("john", ts, "initial comment", "initial comment text")
       val comment2 = Comment("john", ts, "initial comment", "amended comment text")
       
       commentsModel.insertComment("irish", "tune4-reel", comment1)
       commentsModel.insertComment("irish", "tune4-reel", comment2)
       val result = commentsModel.getComment("irish", "tune4-reel", "john", ts)
       
       result.fold(e => failure("retrieval failed: " + e), s => s must_== comment2 )
    }
   
    "allow individual comments to be deleted" in {      
       val ts = String.valueOf(System.currentTimeMillis())
       val comment = Comment("john", ts, "initial comment", "initial comment text")
       
       commentsModel.insertComment("irish", "tune5-reel", comment)
       val result = commentsModel.deleteComment("irish", "tune5-reel", comment.user, comment.cid)
       
       result.fold(e => failure("deletion failed"), s => s must_== (s"Comment deleted user: john id: ${ts} for tune: tune5-reel") )
    }     
    
    "allow comment sets to be deleted" in {      
       val ts = String.valueOf(System.currentTimeMillis())
       val comment1 = Comment("john", ts, "comment 1 for set deletion test", "comment 1 for set deletion test")
       val comment2 = Comment("john", ts, "comment 2 for set deletion test", "comment 2 for set deletion test")
       
       commentsModel.insertComment("irish", "tune6-reel", comment1) 
       commentsModel.insertComment("irish", "tune6-reel", comment2)
       
       val result = commentsModel.deleteComments("irish", "tune6-reel")
       
       result.fold(e => failure("deletion failed"), s => s must_== (s"All comments deleted for genre: irish and tune: tune6-reel") )
    }   
    
   
 
  }
}
  
