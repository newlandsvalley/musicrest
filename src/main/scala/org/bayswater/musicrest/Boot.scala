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

import akka.actor.{ActorSystem, Props, Cancellable}
import akka.io.IO
import scala.concurrent.duration._
 
import java.util.concurrent.TimeUnit;
import spray.can.Http
import org.bayswater.musicrest.cache.{CacheClearActor, CacheClearMessage}
import org.bayswater.musicrest.model.TuneModel
// line below should remove feature warninga
import scala.language.postfixOps

object Boot extends App {
  //Use the system's dispatcher as ExecutionContext for the scheduler
  import system.dispatcher
  
  val musicRestSettings = MusicRestSettings    
  
  // exit if we have no database
  val begin = testDBConnection

  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("on-spray-can")   

  // create and start our service actor
  val service = system.actorOf(Props[MusicServiceActor], "music-rest-service") 
  
  if (0 < musicRestSettings.cacheClearInterval) {
    // create and start our cacheClear actor
    val cashClearActor = system.actorOf(Props[CacheClearActor], "cash-clearance-service")
  
    // and send it our messages from the scheduler
    //This will schedule to send the Clear-message the cacheClearActor after 0 min repeating at intervals determined by configuration
    val cancellable =
      system.scheduler.schedule(0 minutes,
        musicRestSettings.cacheClearInterval minutes,
        cashClearActor,
        CacheClearMessage.Clear)
  }
      
  // start a new HTTP server on the configured host and port with our service actor as the handler
  IO(Http) ! Http.Bind(service, interface = musicRestSettings.serverHost, port = musicRestSettings.serverPort)
  
  def testDBConnection = 
    if (!TuneModel().testConnection) {
      System.out.println("Could not connect to Mongo")
      System.exit(0)
  }  
}
