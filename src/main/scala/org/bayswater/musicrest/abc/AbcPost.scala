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

import java.nio.CharBuffer
import java.net.URLDecoder

import spray.http._
import MediaTypes._
import spray.httpx.unmarshalling.Unmarshaller

/* a simple focus for unmarshalling */
case class AbcPost(notes: String)
case class AlternativeTitlePost(title: String)

object AbcPost {  
    
  implicit val AbcUnmarshaller =
    Unmarshaller[AbcPost](`application/x-www-form-urlencoded`) {
      case HttpBody(contentType, buffer) => {         
        val abcPost =  new String(buffer, contentType.charset.nioCharset)
        // println(s"abc post: $abcPost")
        val encodedNotes = normalise(abcPost)
        val notes = URLDecoder.decode(encodedNotes, "UTF-8")
        // println(s"decoded notes: $notes")
        AbcPost(notes)
      }
    }

  // cater both for url-form encoded and plain text
  def normalise(post: String) :String = if (post.startsWith("abc=")) post.drop(4) else post
}

object AlternativeTitlePost {  
  
  implicit val AlternativeTitleUnmarshaller =
    Unmarshaller[AlternativeTitlePost](`application/x-www-form-urlencoded`) {
      case HttpBody(contentType, buffer) => {         
        val altTitlePost =  new String(buffer, contentType.charset.nioCharset)
        // println(s"alt title post: $altTitlePost")
        val encodedTitle = normalise(altTitlePost)
        val title = URLDecoder.decode(encodedTitle, "UTF-8")
        // println(s"decoded title: $title")
        AlternativeTitlePost(title)
      }
    }  
    
    // cater both for url-form encoded and plain text
    def normalise(post: String) :String = if (post.startsWith("title=")) post.drop(6) else post
}

