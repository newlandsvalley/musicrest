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
import spray.httpx.marshalling._
import org.bayswater.musicrest.model.TuneModel

case class TuneId(name: String, rhythm: String)

object TuneId {

  def apply(id: String): TuneId = parseId(id)

  /** a tune id is of the form "name-genre".
   *  Separate this into its components and return as a TuneId
   */
  private def parseId(id: String): TuneId = {
    val hyphenPos = id.lastIndexOf('-')
    hyphenPos match {
      case -1 => TuneId(id, "unknown")
      case n  => TuneId(id.slice(0,n), id.slice(n+1, id.length))
    }
  }
}

/** A List of tunes, each of which is indexed by a TuneRef
    different return formats have different fields
*/
class TuneList(i: Iterator[scala.collection.Map[String, String]], genre: String, searchParams: Map[String, String], page: Int, size: Int) {


  /** Return tuneId, timestamp, abcHeaders and abc (body)
      This is what the PureScript tunebank-frontend uses which allows
      thumbnails can be created
  */
  def toJSON: String = {
    val quotedi = i.map( cols => cols.map( c => c match {
      case (TuneModel.tuneKey, x) => TuneRef.formatId(x)
      case (a, b)     => TuneRef.formatJSON(a, b)
    }).mkString("{", ",", "}"))
    quotedi.mkString("{ \"tunes\": [", ",", "], " + pageInfoJSON + "  }")
  }

  /** return the tune list as a set of li items (not used) */
  def toHTMLList: String = {
    val quotedi = i.map( cols => buildHtmlLi(cols) )
    val prefix = "<span class=\"tunelist\" " + pageInfoHTML+ ">\n"
    val suffix = "</span>"
    quotedi.mkString(prefix,  " ", suffix)
  }

  /** return the tune list as a set of tabular tr row items
      This is what TradTuneDB uses - a named subset of fields without
      "abc" and "abcHeaders"
  */
  def toHTML: String = {
    val endSpan = "</span>\n"
    if (0 == totalResults) {
      val span = "<span class=\"tunelist\" >\n"
      span + "<p>no matches</p>" + endSpan
    }
    else {
      val span = "<span class=\"tunelist\" " + pageInfoHTML+ ">\n"
      val quotedi = i.map( cols => buildHtmlTr(cols) )
      val prefix = span + "<table>\n"
      val suffix = "</table>\n" + endSpan
      quotedi.mkString(prefix,  " ", suffix)
    }
  }

  def toXML: String = {
      val jvalue = JsonParser.parse(toJSON)
      "<tunes>" + Xml.toXml(jvalue).toString + "</tunes>"
  }

  /* supports json, html and xml at the moment */
  def to(mediaType:MediaType): String = {
      val formatExtension:String = mediaType.subType
      formatExtension match {
          case "json" => toJSON
          case "html" => toHTML
          case "xml" =>  toXML
          // case "plain" =>  s.mkString(",")
          case _ => "Unsupported media type: " + formatExtension
        }
    }

  lazy val totalResults: Long = TuneModel().count(genre, searchParams)

  val maxPages = (totalResults + size - 1) / size


  /** format a returned tune from the db list as an html list item (not used) */
  private def buildHtmlLi(cols: scala.collection.Map[String, String]): String = {
    val tuneId = TuneId(cols(TuneModel.tuneKey))
    val urlPrefix = "genre/" + java.net.URLEncoder.encode(genre, "UTF-8") + "/tune/"
    val id = java.net.URLEncoder.encode(cols(TuneModel.tuneKey), "UTF-8")
    "<li><a href=\""  +urlPrefix + id +"\" >"  + tuneId.name + "</a>" + " (" + tuneId.rhythm + ")"+ "</li>\n"
  }

  /** format a returned tune from the db list as an html table row item */
  private def buildHtmlTr(cols: scala.collection.Map[String, String]): String = {
    val tuneId = TuneId(cols(TuneModel.tuneKey))
    val ts = cols("ts")
    val otherTitles = cols.getOrElse("otherTitles", "")
    val dateSubmitted = formatDate(ts.toLong)
    val urlPrefix = "genre/" + java.net.URLEncoder.encode(genre, "UTF-8") + "/tune/"
    val id = java.net.URLEncoder.encode(cols(TuneModel.tuneKey), "UTF-8")
    "<tr><td><a href=\""  +urlPrefix + id +"\" >"  + tuneId.name + "</a>" + otherTitles +  "</td><td>" + tuneId.rhythm + "</td><td>" + dateSubmitted + "</td>"  + "</tr>\n"
    //"<tr><td><a href=\""  +urlPrefix + id +"\" >"  + tuneId.name + "</a></td><td>" + tuneId.rhythm + "</td><td>" + dateSubmitted + "</td>"  + "</tr>\n"
  }

  private val pageInfoJSON:String = s""""pagination" : { "page": "$page", "size": "$size", "maxPages": "$maxPages" }"""
  private val pageInfoHTML:String = "page=\"" + page + "\" " + "size=\"" + size + "\" "

  private def formatDate(ts: Long): String = {
    val theDate: java.util.Date = new java.util.Date(ts)
    val format = new java.text.SimpleDateFormat("dd-MM-yyyy")
    format.format(theDate)
  }



}

object TuneList {
  def apply(genre: String, sort: String, page: Int, size: Int):TuneList =
      new TuneList(TuneModel().getTunes(genre, sort, page, size), genre, Map.empty[String, String], page, size)

  def apply(genre: String, params: Map[String, String], sort: String, page: Int, size: Int):TuneList =
      new TuneList(TuneModel().search(genre, params, sort, page, size), genre, params, page, size)

   implicit val TuneListMarshaller = {

     val canMarshalTo = Array (ContentTypes.`text/plain`,
                               ContentTypes.`application/json`,
                               ContentType(MediaTypes.`text/xml`),
                               ContentType(MediaTypes.`text/html`))

     Marshaller.of[TuneList] (canMarshalTo:_*) { (value, requestedContentType, ctx) ⇒ {
       val content:String = value.to(requestedContentType.mediaType)
       ctx.marshalTo(HttpEntity(requestedContentType, content))
       }
     }
   }

}
