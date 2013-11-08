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

package org.bayswater.musicrest.model

import org.bayswater.musicrest.Util._
import net.liftweb.json._
import spray.http._
import spray.httpx.marshalling._

case class Comment(user: String, cid: String, subject: String, text: String) {
  val toMap =
     Map("user" -> user, "cid" -> cid, "subject" -> subject, "text" -> text)
     
  def toJSON: String = {
    val atts = for (k <- toMap.keys) yield {
      formatJSON(k, toMap(k))
    }
    val json = "{" +  atts.mkString(",") +  "}" 
    json
  }
  
  def toXML: String = {
      val jvalue = JsonParser.parse(toJSON)
      "<comment>" + Xml.toXml(jvalue).toString + "</comment>"
  }  
  
  /* supports json and xml at the moment */
  def to(mediaType:MediaType): String = {
      val formatExtension:String = mediaType.subType
      formatExtension match {
          case "json" => toJSON
          case "xml" =>  toXML
          case _ => "Unsupported media type: " + formatExtension
        }
    }          
}

object Comment {
  def apply(mdbo: com.mongodb.DBObject): Comment = {
    Comment(mdbo.get("user").asInstanceOf[String], 
            mdbo.get("cid").asInstanceOf[String], 
            mdbo.get("subject").asInstanceOf[String], 
            mdbo.get("text").asInstanceOf[String])
  } 
  
  implicit val commentMarshaller = {    

     val canMarshalTo = Array (ContentTypes.`application/json`,
                               ContentType(MediaTypes.`text/xml`))

     Marshaller.of[Comment] (canMarshalTo:_*) { (value, requestedContentType, ctx) ⇒ {
       val content:String = value.to(requestedContentType.mediaType)
       ctx.marshalTo(HttpEntity(requestedContentType, content))
       }
     }
   }
  
}
