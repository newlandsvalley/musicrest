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

import scala.util.matching.Regex
import scala.collection.mutable.{StringBuilder, MapBuilder}
import scalaz._
import Scalaz._
import spray.http.MediaType
import com.mongodb.casbah.Imports._

import java.io.{File,BufferedInputStream}
import org.bayswater.musicrest.typeconversion.Transcoder
import net.liftweb.json._
import org.bayswater.musicrest.model.TuneModel

import scala.concurrent.{ ExecutionContext, Future, Promise }

/** useful intermediate ABC representation to/from Mongo */
case class AbcMongo(titles: List[String], kvs: scala.collection.Map[String,String], genre: String ) {
  def mongoTitles = MongoDBList.concat(titles)
}

/** a binary image of a transcoded ABC tune */
case class BinaryImage(mediaType: MediaType, stream: BufferedInputStream)

class Abc(val titles: List[String], rhythm: String, headers: scala.collection.Map[String, String], 
          abcDirectives: Option[String], abcHeaders: String, abcBody:String, genre: String)  {
  
    // println(s"ABC titles $titles")
    val name = titles.head
    val tuneType = rhythm
    val id = (name + "-" + tuneType).toLowerCase
    // val safeFileName = name.filter(_.isLetterOrDigit)
    val safeFileName = id.filter(_.isLetterOrDigit)
    val submitter = headers.get("submitter").map(x => s"\nsubmitted by $x\n")
    val abc = abcDirectives.getOrElse("") + abcHeaders + abcBody
    
    /** an option to stamp the ABC with an S header containing its musicrest URI */
    def abcWithSHeader = {
      val newHeaders =
        if (!headers.contains("S")) {
           abcHeaders + "S: " + TuneRef.formatUri(genre, id) + "\n"  
        }
        else {
           abcHeaders
        }
      abcDirectives.getOrElse("") + newHeaders + abcBody
    }
   

    def key:Option[String] = headers.get("K")
    def noteLength:Option[String] = headers.get("L")
    def timeSignature:Option[String] = headers.get("M")
    def author:Option[String] = headers.get("Z")
    def tuneIndex:Option[String] = headers.get("X") 
    def alternativeTitles = titles.tail
    
  
    // for binary media types (pdf, ps, midi, png)
    def to(mediaType:MediaType): Validation[String, BinaryImage] = Transcoder.transcode(safeFileName, mediaType, genre,  abc.lines)   
    
    /** experimental for binary media types (pdf, ps, midi, png) */
    def toFutureBinary(mediaType:MediaType) (implicit executor: ExecutionContext) : Future[Validation[String, BinaryImage]] = Future {
      Transcoder.transcode(safeFileName, mediaType, genre, abc.lines)  
    }
    
       
    /* for streamed media types (wav)
     * 
     * I have chosen not to support integer values for the instruments (see http://ifdo.pugmarks.com/~seymour/runabc/abcguide/abc2midi_guide.html)
     * in favour of explicitly naming those I wish to support.  This is because some don't render at all, some are ridiculous (e.g. helicopter) and
     * some just sound dreadful and nothing like their intended target.
     */
    def toFile(mediaType:MediaType, instrument: String, transpose: Int, tempo: String): Validation[String, File] = {
      val midiInstruments: Map[String, Int] = Map( "piano" -> 1, 
                                                   "harpsichord" -> 6,
                                                   "accordion" -> 21,
                                                   "cello" -> 42,
                                                   "clarinet" -> 71,
                                                   "dulcimer" -> 15,
                                                   "flute" -> 74,
                                                   "guitar" -> 26,  
                                                   "harp" -> 46,
                                                   "bandoneon" -> 23)
                       
      val midiProcess = midiInstruments.getOrElse(instrument, 1)
      Transcoder.transcodeStream(safeFileName, genre, mediaType.subType,  abcHeaders, abcBody, midiProcess, instrument, transpose, tempo)  
    }
    
     /* supports plain text, json and xml at the moment */
    def toStr(mediaType:MediaType): Validation[String, String] = {
      val formatExtension:String = mediaType.subType
      // println("format extension is" + formatExtension)
      formatExtension match {
          case "json" => toJSON.success
          case "xml" =>  toXML.success
          case "html" =>  toHTML.success
          case "plain" =>  abcWithSHeader.success
          case "vnd.abc" =>  abcWithSHeader.success
          case _ => ("Unsupported media type: " + formatExtension).fail
        }
    }  
    
    def toHTML: String = (abc + submitter.getOrElse("")).foldLeft("")((b, c) => c match {
      case '<' => b + "&lt;"
      case '>' => b + "&gt;"
      case '\n' => b +  "<br />"
      case x => b + x
      }
    )
    
    /** build a mongo-friendly version of the ABC notation */
    def toAbcMongo: AbcMongo = AbcMongo(titles, toMap, genre)      
    
    /** map includes everything except the titles */
    private def toMap: Map[String, String] = {
      val mb = new MapBuilder[String, String, Map[String,String]](Map.empty)
      mb ++= headers
      // mb += "T" -> name  (now have it in AbcMongo.titles)
      mb += "abcHeaders" -> abcHeaders
      mb += "abc" -> abcBody
      abcDirectives.foreach(d => mb += "abcDirectives" -> d)
      mb.result
    }
    
   def toJSON: String = {
        val sb = new StringBuilder()
        val encodedHeaders = for ( (key, value) <- headers ) yield ("     " + enQuote(key) + ": " + enQuote(value))
        sb.append("{\n")
        sb.append("\n     " + enQuote("T") + ": " + enQuote(name) + "\n")
        // append the headers (explicitly typed as JSON attributes)
        sb.append(encodedHeaders.mkString(",\n"))
        sb.append(",")
        sb.append("\n     " + enQuote("abc") + ": " + enQuote(abc) + "\n")
        sb.append("\n}\n")
        // println(sb.toString())
        sb.toString
    }    
   
    def addAlternativeTitle(title: String) : Validation[String, Abc] = {
      if (titles.contains(title)) {
        // println("title: " + title + " already exists")
        ("title: " + title + " already exists").fail
      }
      else if (title.contains('\\')) {
          (s"Please use Unicode for all titles and don't use backslashes - you have submitted $title").fail
      }
      else {
        val newAbcHeaders = abcHeaders + ("T: " + title + "\n")
        val newTitles = titles :+ title
        val abc = new Abc(newTitles, rhythm, headers, abcDirectives, newAbcHeaders, abcBody, genre)
        abc.success
      }
    }
   
    def createTemporaryImageFile(): Validation[String, String] = Transcoder.createTemporaryImageFile(safeFileName, genre, abc.lines)   
    
   
    private def toXML: String  = {
      val jvalue = JsonParser.parse(toJSON)
      "<tune>" + Xml.toXml(jvalue).toString + "</tune>"
    }


    
    private def enQuote(s:String): String = "\"" + s + "\""    
    
    def insertIfNew(genre: String): Validation[String, TuneRef] = 
      if (TuneModel().exists(genre, id)) {        
        ("Tune: " + id + " already exists ").fail
      } 
      else {
        TuneModel().insert(genre, this) flatMap ( t => (TuneRef(name, tuneType, id)).success)
      }    
    
    def validTuneRef(genre: String): Validation[String, TuneRef] = (TuneRef(name, tuneType, id)).success 
   
}

object Abc {
    // build from an ABC submission (i.e. supplied from an HTML form)
    
    def apply (titles: List[String], rhythm: String, headers: collection.mutable.HashMap[String, String], 
              abcDirectives: Option[String], abcHeaders:String, abcBody:String, genre: String): Abc = 
      new Abc (titles, rhythm, headers, abcDirectives, abcHeaders, abcBody, genre)
    
    // build from a mongo-friendly representation of the tune (this will eventually supersede the method below)
    def apply(abcMongo: AbcMongo): Abc = {
      val titles = abcMongo.titles      
      val genre = abcMongo.genre
      val rhythm:String = abcMongo.kvs.get("R").getOrElse("rhythmless")
      val abcDirectives:Option[String] = abcMongo.kvs.get("abcDirectives")
      val abcHeaders:String = abcMongo.kvs.get("abcHeaders").getOrElse("headerless")
      val abcBody:String = abcMongo.kvs.get("abc").getOrElse("noteless")
      val headers: scala.collection.Map[String, String] = abcMongo.kvs.filter( (kv) => ! List("abc", "abcHeaders", "abcDirectives").contains(kv._1)   )
      new Abc(titles, rhythm, headers, abcDirectives, abcHeaders, abcBody, genre)
    }
    

    // build a (presumed valid) tune from the BSON-provided map
    def validTune(requestedTune: Tune, optMap: Option[AbcMongo]): Validation[String, Abc] = optMap match {
      case None => (s"Not Found: $requestedTune").fail
      case Some(m) => apply(m).success
    }    
  
}


/* @param genre the tune genre
 * @param titles the tuner titles (from the T parameters)
 * @param headers: the individual headers mapped as n,v pairs other than the T headers
 * @param abcHeaders the headers represented as a simple string from the original ABC
 * @param abcDirectives (abc directives - rare)
 * @param abcBody the rest of the ABC other than the headers and directives)
 * @param abcCount try to keepm tabs on the number of tunes in the ABC - should always be 1 
 */

class AbcSubmission (genre: String,
                     titles: scala.collection.mutable.ListBuffer[String],
                     headers: collection.mutable.HashMap[String, String],
                     abcHeaders: String,
                     abcDirectives: Option[String],
                     abcBody: String,
                     abcCount: Int
                     ) {     
    // def title:Option[String] = headers.get("T")   
    def id:Option[String] = headers.get("_id") 
    def tuneIndex:Option[String] = headers.get("X")
    def tuneKey:Option[String] = headers.get("K")
    def rhythm:Option[String] = headers.get("R")
    
    private def checkIndex() : Validation[String, AbcSubmission] = tuneIndex match {
      case None => this.success
      case Some(i) => if (i == "1") this.success else ("Invalid index: " + i).fail
    } 
    
    private def checkKeySignature() : Validation[String, AbcSubmission] = tuneKey match {
      case None => "No key Signature present in abc".fail
      case Some(k) => {
        if (4 != k.length)  {
          ("Unrecognized key signature: " + k).fail
        }
        else {
          val key = k.head.toUpper
          val mode = k.slice(1,4).toLowerCase
          if (!List('A', 'B', 'C', 'D', 'E', 'F', 'G').contains(key)) {            
             ("Unrecognized key signature: " + k).fail
          }
          else if (!List("dor", "maj", "min", "mix", "phr").contains(mode)) {            
             ("Unrecognized key signature: " + k).fail
          }
          else
            this.success
        }
      }
    }


    private def checkCount() : Validation[String, AbcSubmission] = abcCount match {
      case 0 => "No tunes present in abc".fail
      case c => if (c == 1) this.success else ("More than one tune supplied: " + c).fail
    }
 
    private def checkTitles() : Validation[String, List[String]] = 
      if (titles.isEmpty) {
        "No title (T header) present in abc".fail
      }
      else {
        // check we're not using the old-fashioned ABC escaped mechanisms in the main title instead of Unicode
        val mainTitle = titles.head
        // println(s"main title is $mainTitle")
        if (mainTitle.contains('\\')) {
          (s"Please use Unicode for all titles and don't use backslashes - you have submitted $mainTitle").fail
        }
        else {
          titles.toList.success
        }          
      }
    
    
    
    private def checkRhythm() : Validation[String, String] = rhythm match {
      case None => "No rhythm (R header) present in abc".fail
      case Some(r) => {
          if (SupportedGenres.isRhythm(genre, r.toLowerCase()))
            r.success
          else 
            (r + " is not a recognized rhythm for the " + genre + " genre").fail
      }
    }        
      
    def validate() : Validation[String, Abc] = for {
       t <- checkTitles
       r <- checkRhythm
       _ <- checkKeySignature
       _ <- checkCount
    } yield (Abc(t, r, headers, abcDirectives, abcHeaders, abcBody, genre))
}


object AbcSubmission {
  
    def apply (lines: Iterator[String], genre: String, submitter: String): AbcSubmission = {
      // this should match %%
      val directiveExtractor = """^(%%)(.*)""".r
      val titleExtractor = """^(T):(.*)""".r
      val rhythmExtractor = """^(R):(.*)""".r
      val tuneKeyExtractor = """^(K):(.*)""".r
      val indexExtractor = """^(X):(.*)""".r
      val headerExtractor = """^([a-zA-Z]{1}):(.*)""".r
      val sbDirectives = new StringBuilder()  
      val sbHeaders = new StringBuilder()  
      val sbBody = new StringBuilder()

      val headers = collection.mutable.HashMap[String, String]() 
      val titles = scala.collection.mutable.ListBuffer[String]()
      var abcCount = 0
      var headerMode = true

      lines.foreach{ line => {        
        val cleanLine = replaceDoubleQuotes(line)
        
        cleanLine match {
          case directiveExtractor(dname, dvalue) => {   
            // println("Got Directive: " + dvalue)
            sbDirectives.append(cleanLine + "\n")
          }
          // we'll treat the title header separately
          case titleExtractor(hname, hvalue) => {
            // println("Got Title: " + hvalue)
            titles.append ( hvalue.trim() )
            sbHeaders.append(cleanLine + "\n")
            }
          case tuneKeyExtractor(hname, hvalue) => {
            // normalise major and minor key representations
            val kvalue = hvalue.trim()
            val tuneKey = 
               if (List("A", "B", "C", "D", "E", "F", "G").contains(kvalue))
                 kvalue + "maj"
               else if (List("Am", "Bm", "Cm", "Dm", "Em", "Fm", "Gm").contains(kvalue))
                 kvalue + "in"
               else
                 kvalue
            /** this behaves differently depending on whether it's an initial header or a key change mid tune */
            if (headerMode) {
              headers += (hname.trim() -> tuneKey )
              sbHeaders.append("K:" + tuneKey + "\n")
              // println("set header rhythm to " + tuneKey)
              }
            else {              
              sbBody.append("K:" + tuneKey + "\n")
              // println("set body rhythm to " + tuneKey)
              }
            }
          case indexExtractor(hname, hvalue) => {
            headers += (hname.trim() -> hvalue.trim() )
            abcCount += 1
            sbHeaders.append(cleanLine + "\n")
            }
          case headerExtractor(hname, hvalue) => {
            headers += (hname.trim() -> hvalue.trim() )
            sbHeaders.append(cleanLine + "\n")
            }
          case _ => {
            // System.out.println("not a header " + line)
            // if we have anything here, it must be the start of the tune body
            if (0 < cleanLine.length) {
               headerMode = false
            }
            sbBody.append(cleanLine + "\n")
            }
          }
        }
      }
      // add a timestamp
      headers += ("ts" -> System.currentTimeMillis().toString)
      // add the identity of the submitter
      headers += ("submitter" -> submitter)  
      val directives = 
         if (sbDirectives.length() > 0) {
           Some(sbDirectives.toString)
         }
         else None
         
      // println(s"ABC submission titles: $titles")
      new AbcSubmission(genre, titles, headers, sbHeaders.toString, directives, sbBody.toString, abcCount)
    }      
    
    // JSON doesn't allow embedded double quotes so we'll replace with single quotes 
    def replaceDoubleQuotes(s:String): String = s.foldLeft("")((b, a) => if (a == '"') b + ''' else b + a ) 
   

}
