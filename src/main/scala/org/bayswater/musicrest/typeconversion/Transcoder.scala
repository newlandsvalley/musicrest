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

package org.bayswater.musicrest.typeconversion

import scala.sys.process.{Process, ProcessLogger}

import spray.http.MediaType
import java.io.{InputStream, BufferedInputStream, File, FileInputStream}
import scalaz.Validation
import scalaz.Scalaz._
import org.bayswater.musicrest.{MusicRestSettings, Util}
import org.bayswater.musicrest.abc.BinaryImage


object Transcoder {
  
  // private val musicRestSettings = MusicRestSettings
  private def scriptHome = MusicRestSettings.scriptDir
    

  /** Transcode to the requested MIME type.  Return from the cache if it's been previously transcoded.
   *  supports pdf, ps, png, midi at the moment 
   *  */
  def transcode(abcName: String, mediaType: MediaType, genre: String, abc: Iterator[String]): Validation[String, BinaryImage] = {
      
      if (List("pdf", "postscript", "png", "midi").contains(mediaType.subType)) {  
         val formatExtension = if (mediaType.subType == "postscript") "ps" else mediaType.subType
         val filePath = fullFilePath(MusicRestSettings.pdfDirCore, genre, abcName, formatExtension)
         // println("looking for: " + filePath)
         val file = new File(filePath)
         if (file.exists()) {    
            // println("found it")
            val binaryImage = BinaryImage(mediaType, new BufferedInputStream(new FileInputStream(file)))
            binaryImage.success
         }
         else {
           transcodeAbc(abcName, mediaType, genre, abc)
         }
      }
      else ("Unsupported media type: " + mediaType).failure[BinaryImage]
    }   
 
  
  /* supports pdf, ps, png, midi at the moment */
  def transcodeAbc(abcName: String, mediaType: MediaType, genre: String, abc: Iterator[String]): Validation[String, BinaryImage] = {
    if (List("pdf", "postscript", "png", "midi").contains(mediaType.subType)) {      
      val filePath = fullFilePath(MusicRestSettings.abcDirCore, genre, abcName, "abc")
      val file = new File(filePath)
      Util.writeTextFile(file, abc)
      val formatExtension = if (mediaType.subType == "postscript") "ps" else mediaType.subType
      transcode(abcName, mediaType, genre, formatExtension)
    }
    else ("Unsupported media type: " + mediaType).failure[BinaryImage]
  } 
  
  /* supports just wav at the moment */
  def transcodeStream(abcName: String, genre: String, subtype: String, abcHeaders: String, abcBody:String, midiProcess: Int, instrument: String, transpose: Int, tempo: String): Validation[String, File] = {
    val midiProgram = "%%MIDI program " + midiProcess + "\n"  
    val transposition = if (transpose == 0) "" else "%%MIDI transpose " + transpose + "\n" 
    val tempoHeader = s"Q: 1/4=$tempo \n" 
    val abcText = (abcHeaders + tempoHeader + midiProgram + transposition + abcBody)
    val abc: Iterator[String] = abcText.lines
    if (List("wav").contains(subtype)) {          
      // we need to use a name for the abc file in wav transcodings to include the request parameters (instrument, transpose, tempo) 
      val wavName = waveName(abcName, instrument, transpose, tempo) 
      // val filePath = fullFilePath(MusicRestSettings.abcDirPlay, genre, abcName, "abc")
      val filePath = fullFilePath(MusicRestSettings.abcDirPlay, genre, wavName, "abc")
      val file = new File(filePath)
      Util.writeTextFile(file, abc)
      val formatExtension = if (subtype == "postscript") "ps" else subtype
      // transcodeToFile(abcName, genre, formatExtension)
      transcodeToFile(wavName, genre, formatExtension)
    }
    else ("Unsupported media type: " + subtype).failure[File]
  }   
  
  def createTemporaryImageFile(abcName: String, genre: String, abc: Iterator[String]): Validation[String, String] = {   
    val filePath = fullFilePath(MusicRestSettings.abcDirTry, genre, abcName, "abc")
    val file = new File(filePath)
    Util.writeTextFile(file, abc)
    val tempImage: Validation[String, File] =  transcodeAbcToFile(abcName, genre, "png", true)
    tempImage.fold(
        e => e.failure[String]
        ,
        s => abcName.success
        )
  }
  
  /** Transcode the abc to a binary image
   * 
   * @param abcName - the name of the abc tune
   * @param mediaType - the media type requested
   * @paran genre - the tune's genre
   * @param formatExtension - the file type extension (in most cases other than ps/postscript, identical to the mediaType subType)
   */
  private def transcode(abcName: String, mediaType: MediaType, genre: String, formatExtension: String): Validation[String, BinaryImage] = {
    val vf:Validation[String, File] = transcodeAbcToFile(abcName, genre, formatExtension, false)
    /*
    vfd flatMap (f => {    
        val binaryImage = BinaryImage(mediaType, new BufferedInputStream(new FileInputStream(f)))
        binaryImage.success
    })
    */
    vf map (f =>   
        BinaryImage(mediaType, new BufferedInputStream(new FileInputStream(f)))
    )
  }
  
  private def getFileIfExists(abcName: String, genre: String, formatExtension: String): Validation[String, File] = {
    val filePath = MusicRestSettings.pdfDirCore  + "/" + genre + "/" + abcName + "." + formatExtension
    val file = new File(filePath)
    if (file.exists()) {    
      // println("found it")
      file.success
    }    
    else {
      ("file " + filePath + " not cached").failure[File]
    }
  }
  
  /** get the file from the cache if we can or else transcode it 
   *  
   *  Only for wav format at the moment
   *  */
  private def transcodeToFile(abcName: String, genre: String, formatExtension: String): Validation[String, File] = {
    val cachedFile = getFileIfExists(abcName, genre, formatExtension)
    cachedFile.fold(
        e => transcodeAbcToFile(abcName, genre, formatExtension, false)
        ,
        s => cachedFile
        )
  }
  
  
  private def transcodeAbcToFile(abcName: String, genre: String, formatExtension: String, isTry: Boolean): Validation[String, File] = {
    import scala.collection.mutable.StringBuilder 

    val out = new StringBuilder
    val err = new StringBuilder

    val logger = ProcessLogger(
      (o: String) => out.append(o),
      (e: String) => err.append(e))
      
    val scriptName = "abc2" + formatExtension + ".sh"

    val pb = Process(scriptHome + "/" + scriptName + " " + getAbcHome(genre, formatExtension, isTry) + " " + getTargetHome(genre, formatExtension, isTry) + " " + abcName)
    val exitValue = pb.run(logger).exitValue

    exitValue match {
      case 0 => {val fileName = getTargetHome(genre, formatExtension, isTry)  + "/" + abcName + "." + formatExtension
                 val file = new File(fileName)
                 file.success
                 }
      case _ => err.toString.failure[File]
    }
  }
  
  /** work out which ABC (input) directory to use 
   *
   * @param genre -- the tune's genre
   * @param formatExtension - the file suffix
   * @param isTry - true if it's a one-off transcode without saving
   * */
  private def getAbcHome(genre: String, formatExtension: String, isTry: Boolean): String = {
    val baseDir = formatExtension match {  
    case "wav" => MusicRestSettings.abcDirPlay
    case _ => if (isTry) 
        MusicRestSettings.abcDirTry
      else
        MusicRestSettings.abcDirCore
    }
    baseDir + "/" + genre
  }  
  
  /** work out which target (pdf or wav etc) directory to use 
   * 
   * @param genre -- the tune's genre
   * @param formatExtension - the file suffix
   * @param isTry - true if it's a one-off transcode without saving
   * */
  private def getTargetHome(genre: String, formatExtension: String, isTry: Boolean): String = {
    val baseDir = formatExtension match {  
    case "wav" => MusicRestSettings.wavDirPlay
    case _ => if (isTry)
        MusicRestSettings.pdfDirTry
      else
        MusicRestSettings.pdfDirCore
    }
    baseDir + "/" + genre
  }
  
  def fullFilePath(home: String, genre: String, abcName: String, formatExtension: String): String = 
    home + "/" + genre + "/" + abcName + "." + formatExtension
    
  def fullWavPath(home: String, genre: String, abcName: String, instrument: String, transpose: Int, tempo: String): String = 
    fullFilePath(home, genre, waveName(abcName, instrument, transpose, tempo), "wav")
    
  /* We must name wav files not only with the abc name but also with the 
   * parameters used to render it. 
   * 
   */
  private def waveName(abcName: String, instrument: String, transpose: Int, tempo: String) =
      abcName + "-I"  + instrument + "-Q" + tempo + "-T" + transpose 
  
}
