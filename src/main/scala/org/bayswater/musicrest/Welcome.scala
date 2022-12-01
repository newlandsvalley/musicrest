/*
 * Copyright (C) 2011-2022 org.bayswater
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

import spray.http._
import spray.httpx.marshalling._

class Welcome {

  val version = "1.3.2"

  val welcomeMessage = s"MusicRest version $version using spray-routing on spray-can"

  def toXML: String = s"<welcome>$welcomeMessage<welcome>"

  def toHtml: String = s"<h2>$welcomeMessage<h2>"

  def toJSON: String = {
    "{" +
    Util.formatJSON("welcome", welcomeMessage) +  " "
    "}"
  }


  /* supports plain text, html and xml at the moment */
  def to(mediaType:MediaType): String = {
      val formatExtension:String = mediaType.subType
      formatExtension match {
          case "json" => toJSON
          case "xml" =>  toXML
          case "html" => toHtml
          case "plain" =>  welcomeMessage
          case _ => "Unsupported media type: " + formatExtension
        }
    }
}

object Welcome {
  def apply():Welcome = new Welcome()

  implicit val welcomeMarshaller = {

     val canMarshalTo = Array (ContentTypes.`text/plain`,
                               ContentType(MediaTypes.`text/xml`),
                               ContentType(MediaTypes.`text/html`))

     Marshaller.of[Welcome] (canMarshalTo:_*) { (value, requestedContentType, ctx) ⇒ {
       val content:String = value.to(requestedContentType.mediaType)
       ctx.marshalTo(HttpEntity(requestedContentType, content))
       }
     }
   }


}
