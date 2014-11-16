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

package org.bayswater.musicrest

import java.io.{InputStream, BufferedInputStream, File, FileWriter, BufferedWriter, Reader, FileReader, BufferedReader}
import scala.sys.process.{Process, ProcessLogger}
import scalaz.Validation
import scalaz.syntax.validation._

object Util {
  
  /** loan pattern for a (binary) input stream */
  private def withInputStream [A] (is: InputStream)(body: (InputStream) => A) : A = {
    try 
      body(is)
    finally {
      if (null != is) {
        try {
            is.close()
        }      	
        catch {
           case e: Exception  => None
        }
      }
    }
  }		
    
  /** loan pattern for a text file reader */
  private def withReader [A] (r: Reader)(body: (Reader) => A) : A = {
    try 
      body(r)
    finally {
      if (null != r) {
        try {
            r.close()
        }      	
        catch {
           case e: Exception  => None
        }
      }
    }
  }	
  
  private def withBufferedWriter [A] (file: File) (body: (BufferedWriter) => A) : A = {
    val bw = new BufferedWriter( new FileWriter(file))
    try 
      body(bw)
    finally {
      if (null != bw) {
        try {
            bw.close()
            }      	
        catch {
    	    case e: Exception  => None
            }
        }
      }   
   }  
    
  def readInput(bi: BufferedInputStream): Array[Byte] = {
     withInputStream(bi) { bi => 
       val buf = scala.collection.mutable.ListBuffer[Byte]()
       while (bi.available() > 0) {
         buf.append(bi.read().asInstanceOf[Byte])
       }       
       buf.toList.toArray       
     }
  }    
  
  /*
  def readTextFileLines(br: BufferedReader) : Iterator[String] = {
    withReader(br) { br => 
      
    }
  }
  */
  
   def writeTextFile(file:File, lines:Iterator[String]) = 
     withBufferedWriter(file) { bw =>
       lines.foreach(l => {
         bw.write(l)
         bw.write("\n")
       })
     }
   
   /** not needed I think */
   def imageSize(name: String): Validation[String, (String, String)] = {
     val pdfHome = MusicRestSettings.pdfDirCore
     val fileName = pdfHome + "/" + name  + ".png"   
     
     val out = new StringBuilder
     val err = new StringBuilder

     val logger = ProcessLogger(
       (o: String) => out.append(o),
       (e: String) => err.append(e))
       
     val command = "file" + " " + fileName
     println("executing " + command)
       
     val pb = Process(command)
     val exitValue = pb.run(logger).exitValue 
     
     println("output " + out.toString)

     exitValue match {
       case 0 => {
         val extractor = """.*PNG image data, ([0-9]+) x ([0-9]+).*""".r    
         out.toString match {
            case extractor(width, height) => {
              val ans = (width, height)
              println("dimensions: " + ans)
              ans.success
            }
            case _ => "no dimensions".fail
         }
       }
       case _ => err.toString.fail
     }
   }
   
   
   // json requires the backslash to be escaped
   def encodeJSON(s: String) = s.foldLeft(""){
     (b, a) => a match {
        case '\\' => b + "\\\\"
        // I imagine quotes can only arrive already backslashed
        // case '"' => b + "\\\""
        case x => b+ x
     }
   }
   
   def formatJSON (name: String, value: String) : String = "\"" + encodeJSON(name) + "\"" + ": " + "\"" + encodeJSON(value) + "\" "
   
   /* we don't want special URI characters in tune names which form part of a URL */
   def isAcceptableURLCharacter(c: Char): Boolean =
     ((c != '?') && (c != '&') && (c != '#'))
   
      
}  
