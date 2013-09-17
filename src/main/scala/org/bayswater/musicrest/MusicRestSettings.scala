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

import com.typesafe.config.{Config, ConfigFactory}

object MusicRestSettings {
  
  // this relies on setting the -D JVM config.file parameter named config.file (also set in Build.sbt)
  private[this] val c: Config = {
    val c = ConfigFactory.load()
    c.checkValid(ConfigFactory.defaultReference(), "musicrest")
    c.getConfig("musicrest")
  }
  
  /** the spray can server */
  val serverHost          = c getString         "server.host"
  val serverPort          = c getInt            "server.port"
  val thisServer          = serverHost + ":" + serverPort.toInt 
  
  val scriptDir            = resolveRelativeDir(c getString "transcode.scriptDir")
  val transcodeCacheDir    = resolveRelativeDir(c getString "transcode.cacheDir")
  
  val cacheClearInterval  = c getInt          "transcode.cacheClearInterval"
  
  // various subdirectories used by transcoder
  val dirCore          = transcodeCacheDir + "/core"
  val dirTry           = transcodeCacheDir + "/try"
  val dirPlay          = transcodeCacheDir + "/play"
  val abcDirCore       = transcodeCacheDir + "/core/abc"
  val pdfDirCore       = transcodeCacheDir + "/core/pdf"
  val abcDirTry        = transcodeCacheDir + "/try/abc"
  val pdfDirTry        = transcodeCacheDir + "/try/pdf"
  val abcDirPlay       = transcodeCacheDir + "/play/abc"
  val wavDirPlay       = transcodeCacheDir + "/play/wav"
  
  val defaultPageSize  = c getInt          "paging.defaultSize"
  
  val dbName           = c getString       "database.dbName" 
  val dbHost           = c getString       "database.host" 
  val dbPort           = c getInt          "database.port"
  val dbPoolSize       = c getInt          "database.poolSize"
  
  val mailHost         = c getString       "mail.host"
  val mailPort         = c getString       "mail.port"
  val mailLogin        = c getString       "mail.login"
  val mailPassword     = c getString       "mail.password"
  val mailFromAddress  = c getString       "mail.fromAddress"  

  def resolveRelativeDir(dir: String) = 
    if (dir.startsWith("/"))
       dir
    else System.getProperty("user.dir") + "/" + dir   
  
  
}
