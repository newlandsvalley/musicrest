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

// import blueeyes.json.{Xml, JsonParser}
import net.liftweb.json._
import spray.http._
import spray.httpx.marshalling._
import org.bayswater.musicrest.MusicRestSettings

/** A reference to a recently inserted tune, providing, in particular, its uri */
class TuneRef(title: String, rhythm: String, id: String) {
  
  import org.bayswater.musicrest.abc.TuneRef._
  
  def toJSON: String = {
    "{" +
    formatJSON("title", title) +  ", " + 
    formatJSON("rhythm", rhythm) +  ", " + 
    formatId(id) + " " + 
    "}" 
  }
  
  def toXML: String = {
      val jvalue = JsonParser.parse(toJSON)
      "<tune>" + Xml.toXml(jvalue).toString + "</tune>"
  }  
  
  /* supports plain text, json and xml at the moment */
  def to(mediaType:MediaType): String = {
      val formatExtension:String = mediaType.subType
      formatExtension match {
          case "json" => toJSON
          case "xml" =>  toXML
          case "plain" =>  id
          case _ => "Unsupported media type: " + formatExtension
        }
    }     
  
  def uri(genre: String): String  = formatUri(genre, id)    
  
}

object TuneRef {
  def apply(title: String, rhythm: String, id: String):TuneRef = new TuneRef(title: String, rhythm: String, id: String)
  
  // format attributes
  def format(name: String, value: String, separator: String) : String =  "\"" + name + "\"" + separator + "\"" + value + "\" " 
  
  // ditto for ids (ensuring they're URL encoded - useful as relative uris in html links */
  def formatId(id: String) : String = format("uri", java.net.URLEncoder.encode(id, "UTF-8"), ": ")     
    
  // format attributes as JSON
  def formatJSON(name: String, value: String) : String =  format(name, value, ": ") 
  
  // absolute URI
  def formatUri(genre: String, id: String): String = {
    s"""http://${MusicRestSettings.thisServer}/musicrest/genre/$genre/tune/${java.net.URLEncoder.encode(id, "UTF-8")}"""
  }
    

  implicit val tuneRefMarshaller = {    

     val canMarshalTo = Array (ContentTypes.`text/plain`,
                               ContentTypes.`application/json`,
                               ContentType(MediaTypes.`text/xml`),
                               ContentType(MediaTypes.`text/html`))

     Marshaller.of[TuneRef] (canMarshalTo:_*) { (value, requestedContentType, ctx) ⇒ {
       val content:String = value.to(requestedContentType.mediaType)
       ctx.marshalTo(HttpEntity(requestedContentType, content))
       }
     }
   }
  
 
}
