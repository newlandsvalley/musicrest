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

import net.liftweb.json._
import spray.http._
import MediaTypes._
import spray.httpx.marshalling._
import org.bayswater.musicrest.Util._

import org.bayswater.musicrest.model.{UserModel, UserRef}

class UserList(i: Iterator[UserRef], page: Int, size: Int) {
  
  def toJSON: String = {
    val quotedi = i.map( userRef => {
     "{ " +  formatJSON("name", userRef.name) + ", " + formatJSON("email", userRef.email) + "}\n"
    })  
    quotedi.mkString("{ \"user\": [", ",", "], " + pageInfoJSON + "  }") 
  }    
  
   /** return the user list as a set of tabular tr row items  */
  def toHTML: String = {
    val endSpan = "</span>\n"
    if (0 == totalResults) {
      val span = "<span class=\"userlist\" >\n"
      span + "<p>no matches</p>" + endSpan
    }
    else {
      val span = "<span class=\"userlist\" " + pageInfoHTML+ ">\n"
      val quotedi = i.map( u => buildHtmlTr(u) )
      val prefix = span + "<table>\n"
      val suffix = "</table>\n" + endSpan
      quotedi.mkString(prefix,  " ", suffix) 
    }
  }       
  
  def toXML: String = {
      val jvalue = JsonParser.parse(toJSON)
      "<users>" + Xml.toXml(jvalue).toString + "</users>"
  }  
  
 /* supports json and xml at the moment */
  def to(mediaType:MediaType): String = {
      val formatExtension:String = mediaType.subType
      formatExtension match {
          case "json" => toJSON
          case "xml" =>  toXML
          case "html" =>  toHTML
          case _ => "Unsupported media type: " + formatExtension
        }
    }     
  
  lazy val totalResults: Long = UserModel().userCount()   
  
   /** format a returned user from the db list as an html table row item */
  private def buildHtmlTr(u: UserRef): String = 
    s"<tr><td>${u.name}</td><td>${u.email}</td><td>${u.valid}</td></tr>\n"       
 
  // private val pageInfoJSON:String = " \"pagination\" : { \"page\""  + ": \"" + page + "\" ," + "\"size\""  + ": \"" + size + "\" }"
  private val pageInfoJSON:String = s""""pagination" : { "page": "$page", "size": "$size" }"""
  private val pageInfoHTML:String = s"""page="$page" size="$size""""
}

object UserList {
  def apply(page: Int, size: Int): UserList = new UserList(UserModel().getUsers(page, size), page, size) 

  implicit val UserListMarshaller = {  
     val canMarshalTo = Array (ContentType(MediaTypes.`text/xml`),
                               ContentType(MediaTypes.`text/html`),
                               ContentTypes.`application/json`)

     Marshaller.of[UserList] (canMarshalTo:_*) { (value, requestedContentType, ctx) ⇒ {
       val content:String = value.to(requestedContentType.mediaType)
       // println(s"user list requested media type ${requestedContentType.mediaType}")
       ctx.marshalTo(HttpEntity(requestedContentType, content))
       }
     }
   }
  
}
