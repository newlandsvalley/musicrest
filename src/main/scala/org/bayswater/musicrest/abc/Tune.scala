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
import scalaz.Validation
import scalaz.{ \/, -\/, \/- }
import scalaz.syntax.validation._
import spray.http._
import spray.httpx.marshalling._
import spray.util.LoggingContext  
import MediaTypes._
import scala.concurrent.{ ExecutionContext, Future, Promise }
import java.io.{File, BufferedInputStream, FileInputStream}
import org.bayswater.musicrest.typeconversion.Transcoder
import org.bayswater.musicrest.MusicRestSettings
import org.bayswater.musicrest.MusicRestService._


import org.bayswater.musicrest.model.TuneModel
import org.bayswater.musicrest.Util

case class Tune(genre: String, name: String) {  
  // methods that allow over-riding of content negotiation  

  // as Html
  def asHtml: Validation[String, String] =
     Tune.tuneToValidString(this,  ContentType(MediaTypes.`text/html`)) 
     
  // as json
  def asJson: Validation[String, String] =
     Tune.tuneToValidString(this,  ContentTypes.`application/json`) 
     
  // as ABC
  def asAbc: Validation[String, String] =
     Tune.tuneToValidString(this,  ContentType(Tune.AbcType))
     
  // as a future binary representation defined by the requested file type extension
  def asFutureBinary(contentType: ContentType)(implicit executor: ExecutionContext): Future[Validation[String, BinaryImage]] =
     Tune.tuneToFutureBinary(this, contentType) 
     
  // as a future binary representation defined by the requested file type extension
  def asFutureTemporaryImage()(implicit executor: ExecutionContext): Future[Validation[String, BinaryImage]] =
     Tune.tuneToTemporaryPngImage(this)
     
  // as a boolean string value 
  def exists(): String =
    Tune.exists(this).toString
    
  def safeFileName = name.filter(_.isLetterOrDigit) 
}

object Tune {  
  
  case class WavFile(file: File)
  
  // ABC has its own MIME type
  val AbcType = register(
    MediaType.custom(
      mainType = "text",
      subType = "vnd.abc",
      compressible = true,
      binary = false,
      fileExtensions = Seq("abc")))

      
  /** Meta-Marshaller for a Validation.  But, we shouldn't throw an exception here!  Trouble is, currently
   *  with Spray, if (say) we're serving binary content, we can't change our mind and provide a
   *  text/plain error message because we have no access within a marshaller to the response headers - we simply
   *  have to rely on the content negotiation specified by the top level marshaller.
   *  
   *   Need to wait for this fix:
   *   
   * https://github.com/spray/spray/issues/293
   * 
   */
  implicit def validationMarshaller[B](implicit mb: Marshaller[B]) =
    Marshaller[Validation[String, B]] { (value, ctx) ⇒
      value fold (
        e => throw new MusicRestException(e),
        s ⇒ mb(s, ctx)
      )
    }

  /* Marshaller for a Binary Image */
  implicit val binaryImageMarshaller = 
     Marshaller.of[BinaryImage] (`application/pdf`,
                                 `application/postscript`,
                                 `image/png`,
                                 `audio/midi`) {
       (value, requestedContentType, ctx) ⇒ {
          val bytes:Array[Byte] = Util.readInput(value.stream)
           ctx.marshalTo(HttpEntity(requestedContentType, bytes))             
       }
     }

  /* we over-ride the Spray basic String marshaller because we already represent the
   * various string-like content types as Strings.  The default behaviour in Spray is to
   * represent XML as NodeSeqs, and Json as a binary.  I want to simplify the Validation 
   * containers with just two contained types - String(like) and binary
   */
  implicit val stringLikeMarshaller = 
     Marshaller.of[String] (`text/plain`,
                           Tune.AbcType,
                          `text/xml`,
                          `application/json`) {
       (value, requestedContentType, ctx) ⇒ {
         ctx.marshalTo(HttpEntity(requestedContentType, value)) 
        }
     }
  
  /* doesn't work yet because getFromFile is a directive (only works within routes)
   * 
   * we need instead to wait for this release
   *   https://groups.google.com/forum/#!topic/spray-user/VKVDspJjIV8
   * 
  implicit val wavFileMarshaller = 
     Marshaller.of[WavFile] (`audio/wav`) {
       (value, requestedContentType, ctx) ⇒ {
         val chunks = getFromFile(value.file)
         ctx.marshalTo(HttpEntity(requestedContentType, chunks)) 
        }
     }
     * 
     */

 
  // to a Future Binary of type defined by the request fileType extension (from the URL)
  def tuneToFutureBinary (tune: Tune, contentType: ContentType) (implicit executor: ExecutionContext): Future[Validation[String, BinaryImage]] = { 
    val tuneOpt:Option[AbcMongo] = TuneModel().getTune(tune.genre, tune.name)     
    val futOpt = for {
      t <- tuneOpt
    } yield { Abc(t).toFutureBinary(contentType.mediaType) }

    futOpt.getOrElse(Promise.failed(new MusicRestException(tune.name + " not found in genre " + tune.genre + " for " + contentType.mediaType)).future)
  }
  
  /** return the (future) temporary png image of a tune as the result of a 'try tune' transcode attempt */
  def tuneToTemporaryPngImage(tune: Tune) (implicit executor: ExecutionContext): Future[Validation[String, BinaryImage]] = Future {
    val safeFileName = tune.name.filter(_.isLetterOrDigit)
    // val filePath = MusicRestSettings.pdfDirTry + "/" + genre + "/" + safeFileName + "." + "png"    
    val filePath = Transcoder.fullFilePath(MusicRestSettings.pdfDirTry, tune.genre, safeFileName, "png")
    val file = new File(filePath)
    println("looking for temporary tune image at path " + filePath)
    val content:Validation[String, BinaryImage] =  if (file.exists()) {  
       val bis = new BufferedInputStream(new FileInputStream(file))
       val mediaType = ContentType(`image/png`).mediaType
       val binaryImage = BinaryImage(mediaType, bis)
       binaryImage.success
       }
    else ("Temporary image for " + tune.name + " not found at path " + filePath).failure[BinaryImage]
    content 
  } 

  /** return the tune as a wav file 
   *  
   *  I've not yet bothered to wrap in a Future here but I think I should.  Eventually, wav file contents
   *  are returned via getFromFile which runs asynchronously, but there is still quite a computational cost
   *  in first doing the file-based transcoding.
   *  
   *  But, I think I'll wait for this to hit a mainstream release:
   *  
   *  https://groups.google.com/forum/#!searchin/spray-user/getFromFile/spray-user/VKVDspJjIV8/3BMTYoPNc54J
   *  */
  def tuneToWav(tune: Tune, instrument:String, transpose: Int, tempo: String)(implicit log: LoggingContext): Validation[String, File] =  {  
    val tuneOpt:Option[AbcMongo] = TuneModel().getTune(tune.genre, tune.name)
    // convert to validated ABC
    val safeFileName = tune.name.filter(_.isLetterOrDigit)
    val filePath = Transcoder.fullWavPath(MusicRestSettings.wavDirPlay, tune.genre, safeFileName, instrument, transpose, tempo)
    val cachedWavFile = new File(filePath)
    log.info(s"request for wave file $filePath served from cache? ${cachedWavFile.exists}")
    // serve from the cache if we can
    if (cachedWavFile.exists) {
      cachedWavFile.success
    }
    else {
      val validAbc: Validation[String,Abc] = Abc.validTune(tune, tuneOpt) 
      (for 
        {
         abc <- validAbc.disjunction
         f <- abc.toFile(`audio/wav`, instrument, transpose, tempo).disjunction
        } yield f 
      ).validation
    }
  }  

  def tuneToValidString(requestedTune: Tune, requestedContentType: ContentType): Validation[String, String] = {
    // try to get the requested tune from the database
    val tuneOpt:Option[AbcMongo] = TuneModel().getTune(requestedTune.genre, requestedTune.name)
    toValidString(Abc.validTune(requestedTune, tuneOpt), requestedContentType)
  }

  def tuneToValidImage(requestedTune: Tune, requestedContentType: ContentType): Validation[String, BinaryImage] = {
    // try to get the requested tune from the database
    val tuneOpt:Option[AbcMongo] = TuneModel().getTune(requestedTune.genre, requestedTune.name)
    // convert to validated ABC
    toValidImage(Abc.validTune(requestedTune, tuneOpt), requestedContentType)
  }


  def toValidString(validAbc: Validation[String, Abc], requestedContentType: ContentType): Validation[String, String] = 
    (for 
      {
        abc <- validAbc.disjunction
        p <- abc.toStr(requestedContentType.mediaType).disjunction
      } yield p
    ).validation


  def toValidImage(validAbc: Validation[String, Abc], requestedContentType: ContentType): Validation[String, BinaryImage] = 
    (for 
      {
        abc <- validAbc.disjunction
        p <- abc.to(requestedContentType.mediaType).disjunction
      } yield p
    ).validation

  
  def exists(requestedTune: Tune): Boolean = {
    // try to get the requested tune from the database
    // println(s"Existence check for genre: ${requestedTune.genre}, tune: ${requestedTune.name}" )
    val tuneOpt:Option[AbcMongo] = TuneModel().getTune(requestedTune.genre, requestedTune.name)
    tuneOpt.map(x => true).getOrElse(false)
  }

  implicit def tuneMarshaller(implicit mBinaryImage: Marshaller[Validation[String, BinaryImage]], 
                                mString: Marshaller[Validation[String, String]]) = 
      Marshaller.of[Tune](
                          `text/plain`,
                          `text/xml`,
                           Tune.AbcType,
                          `application/json`,
                          `application/pdf`,
                          `application/postscript`,
                          `image/png`,
                          `audio/midi`) { 
        (value, contentType, ctx) ⇒           
          if (contentType.mediaType.binary && contentType.mediaType != `application/json`) {
             // println("tune marshaller requested tune binary content type "+ contentType)
             val image = tuneToValidImage(value,contentType)
             mBinaryImage(image, ctx)
          }
          else {
             // println("tune marshaller requested tune string content type "+ contentType)
             val s = tuneToValidString(value,contentType)
             mString(s, ctx)
          } 
      } 
    
}
