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

package org.bayswater.musicrest.cache

import java.io.File
import akka.actor.UntypedActor
import akka.actor.{Actor, ActorLogging}
import org.bayswater.musicrest.MusicRestSettings

object CacheClearMessage {
  case object Clear
  case object Done
}

class CacheClearActor extends Actor with ActorLogging {
   import Cache._
  
   def receive = {
     case CacheClearMessage.Clear ⇒ {
       // log.info("Clearing the cache")
       val dir = new File(MusicRestSettings.transcodeCacheDir)
       val maxCacheSize = MusicRestSettings.cacheMaxSizeMb
       clearCache(dir, maxCacheSize)
     }
    
     case CacheClearMessage.Done ⇒ context.stop(self)
   } 
}

object Cache {
  import spray.util.LoggingContext 
  
  def cacheSizeInMb(dir: File): Long
    = size(dir) / 1048576
  
  /* Return the size of a file or directory
   * 
   */
  def size(f: File): Long =
    if (f.isDirectory()) {
      size(Some(f.listFiles()))
    }
    else {
      // println(s"found ${f.getName()} of length ${f.length()}")
      f.length()
    }

   /* Return the size of a list of directory contents
    * 
    */
   private def size(ofs: Option[Array[File]]): Long =
     ofs match {
        case None => 0:Long
        case Some(fs) =>
           fs.foldLeft(0:Long)((acc, f) => acc + size(f))
     } 
      
  
  private def removeFromCache(dir: File)(filter: File => Boolean)(implicit log: LoggingContext) :Unit = {  
     // println(s"clearing cache: $dir")   
       
     for {
       files <- Option(dir.listFiles)
       file <- files 
       if (filter(file)) 
     }  file.delete() 
     
     for {
       files <- Option(dir.listFiles)
       subdir <- files 
       if (subdir.isDirectory)                        
     } removeFromCache(subdir)(filter)
   }
   
  /** Delete all files that might be collected in the cache that are
   *  more than 30 seconds old (to allow for in-flight transactions)
   * 
   */
   val allFiles: File => Boolean = 
     file => {
       val age = (System.currentTimeMillis - file.lastModified) / 1000
       // println(s"File name ${file.getName}  age: $age s")
       (file.isFile && 
       (age > 30)   &&  (file.getName.endsWith(".abc") ||
                         file.getName.endsWith(".pdf")  ||
                         file.getName.endsWith(".ps")   ||
                         file.getName.endsWith(".png")  ||
                         file.getName.endsWith(".midi") ||
                         file.getName.endsWith(".wav")  
                        ))
   }
                        
   private def matchingFiles(prefix: String): File => Boolean =
     file => (file.isFile && file.getName.startsWith(prefix))
     
   def clearCache(dir: File, maxCacheSize: Int)(implicit log: LoggingContext) = {
     val cacheSize = cacheSizeInMb(dir)
     if (cacheSize > maxCacheSize) {
        log.info(s"Clearing cache (size: ${cacheSize}mb, max: ${maxCacheSize}mb)")
        removeFromCache(dir)(allFiles)
     }
   }
      
   def clearTuneFromCache(dir: File, tuneName: String)(implicit log: LoggingContext) =  {
      val safeFileName = tuneName.filter(_.isLetterOrDigit)
      log.debug(s"attempting to remove $safeFileName from cache")
      removeFromCache(dir)(matchingFiles(safeFileName))
   } 
}