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
import scalaz.Validation
import scalaz.Scalaz._
import org.bayswater.musicrest.model.TuneModel
import org.bayswater.musicrest.MusicRestSettings
import org.bayswater.musicrest.Util._

// class GenreList(s: scala.collection.Set[String]) {  
class GenreList(s: List[String]) {  
  
  def toJSON: String = {
    val quoteds = s.map( x => "\"" + encodeJSON(x) + "\"")
    "{ " + "\"genre\"" + " :" + quoteds.mkString("[", ",", "]") + " }"
  }
  
  def toXML: String = {
      val jvalue = JsonParser.parse(toJSON)
      "<genres>" + Xml.toXml(jvalue).toString + "</genres>"
  }
  
  def toHTML: String = s.foldLeft("")((b, c) => c match {
      case x => b + "<p>" + x + "<br /></p>" 
      }
  )
  
  
  /* supports plain text, json and xml at the moment */
  def to(mediaType:MediaType): String = {
      val formatExtension:String = mediaType.subType
      formatExtension match {
          case "json" => toJSON
          case "xml" =>  toXML
          case "html" =>  toHTML
          case "plain" =>  s.mkString(",").trim
          case _ => "Unsupported media type: " + formatExtension
        }
    }   

}

object GenreList {
    
  // def apply():GenreList = new GenreList(TuneModel().getGenres) 
  def apply():GenreList = new GenreList(SupportedGenres.genres) 
  
  def validate(genre:String): Validation[String, String] = 
    // if (MusicRestSettings.genres.contains(genre))
    if (SupportedGenres.isGenre(genre))
      genre.success
    else
      ("Genre: " + genre + " is not currently supported").failure[String]  
 

   implicit val GenreListMarshaller = {  
     val canMarshalTo = Array (ContentTypes.`text/plain`,
                               ContentTypes.`application/json`,
                               ContentType(MediaTypes.`text/xml`),
                               ContentType(MediaTypes.`text/html`))

     Marshaller.of[GenreList] (canMarshalTo:_*) { (value, requestedContentType, ctx) ⇒ {
       val content:String = value.to(requestedContentType.mediaType)
       ctx.marshalTo(HttpEntity(requestedContentType, content))
       }
     }
   }
    
  
}
