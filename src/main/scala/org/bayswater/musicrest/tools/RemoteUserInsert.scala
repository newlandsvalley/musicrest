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

package org.bayswater.musicrest.tools

import scala.concurrent.Future  
import scala.util.Try
import scala.util.{Success, Failure}
import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import spray.http._
import spray.can.Http
import HttpMethods._
import spray.httpx.RequestBuilding._
import spray.http.HttpHeaders._
import java.net.URLEncoder

object RemoteUserInsert {
  
  def main(args: Array[String]): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    if (args.length < 5) {      
      println("Usage: RemoteUserInsert <server> <admin password> <user name> <user password> <email>")
      System.exit(0)
    }    
    
    val serverName = args(0)
    val adminPassword = args(1)
    val uName = args(2)    
    val password = args(3)
    val email = args(4)
    
    implicit val system = ActorSystem("user-add")
    
    val future = insertUser(serverName, adminPassword, uName, password, email)        
    
    future.onComplete{
      case Success(res) => println(s"insertion result, status: ${res.status} text: ${res.entity.asString}")
      // failure can't really happen in this future
      case Failure(error) => println(s"failed insertion: $error")
    }
    
    future.onComplete { _ => system.terminate() }
  }
  
 
  
  def insertUser(serverName: String, adminPassword: String, uName: String, password: String, email: String)(implicit system: ActorSystem) : Future[HttpResponse] = {
    implicit val timeout: Timeout = 5.seconds      
   
    import system.dispatcher // execution context for future transformation below
      
    val form = newUserFormData(uName, email, password)
    val auth = Authorization(BasicHttpCredentials("administrator", adminPassword))
    val request = Post(s"http://${serverName}/musicrest/user", form)
    
    for {
      response <- IO(Http).ask(request ~> auth).mapTo[HttpResponse]
    _   <- IO(Http) ? Http.CloseAll
    } yield { response }      
  }    
  
  def newUserFormData(name: String, email: String, password: String) =
    FormData(Map("name" -> URLEncoder.encode(name, "UTF-8"),
                "email" -> URLEncoder.encode(email, "UTF-8"),
                "password" -> URLEncoder.encode(password, "UTF-8"),
                "password2" -> URLEncoder.encode(password, "UTF-8")))
}
