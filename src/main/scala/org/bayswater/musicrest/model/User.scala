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

package org.bayswater.musicrest.model

import java.util.UUID;
import scala.util.matching.Regex
import scalaz._
import Scalaz._

import spray.http._
import MediaTypes._
import spray.httpx.marshalling._

case class User(name:String, email:String, password:String) {
  
  val uuid:String = UUID.randomUUID().toString()
  
  def insert(doRegister: Boolean): Validation[String, UnregisteredUser] = TuneModel().insertUser(this, doRegister)    
}

case class UserRef(name:String, email:String, password:String) 

/* an unregistered user is one who is on, or about to be placed on the user database
 * but who has not necessarily registered by clicking on the registration email link
 */
case class UnregisteredUser(name: String, uuid: String)

object User {
  
  def checkName(name:String): Validation[String, String] = {
    val userformat = """^[A-Za-z]([A-Za-z0-9_-]){3,24}$""".r
    if (userformat.pattern.matcher(name)matches)
      name.success
    else
      (s"Bad format for user $name - names should start with a letter, be at least 4 characters in length and may contain letters numbers, underscore and minus").fail
  }
  
  def checkUnique(name:String): Validation[String, String] = {
    val userRef: Validation[String, UserRef] = TuneModel().getUser(name)
    userRef.fold(
        e => name.success,
        u => ("User " + name + " already exists").fail
        )
  }
   
  def checkPassword(password:String, password2:String) : Validation[String, String] = 
    if (password === password2) {
       if (password.length < 7) "password must have at least 7 characters".fail
       else if (!password.exists(_.isDigit)) "password must contain at least one digit".fail
       else if (!password.exists(_.isLetter)) "password must contain at least one letter".fail
       else password.success
     }
     else
       "passwords must be identical".fail
  
  
  def checkEmail(email:String): Validation[String, String] = {
    /* 
    val pattern = """^.+@[A-Z0-9.-]+\.[A-Z]{2,4}$""".r
    email match {
      case pattern(x) => email.success
      case _ => "Invalid email address".fail
    }
    */
    if (email.contains('@')) email.success else ("Invalid email address: " + email).fail
  }
  
  def validate(uuid: String): Validation[String, UnregisteredUser] = TuneModel().validateUser(uuid)
  
  def get(uuid: String): Validation[String, UserRef] = TuneModel().getUser(uuid)
  
}

object UnregisteredUser { implicit val UserListMarshaller = {  
    
     Marshaller.of[UnregisteredUser] (`text/html`) { (value, requestedContentType, ctx) ⇒ {
       val content:String =  s"User: ${value.name} OK" 
       ctx.marshalTo(HttpEntity(requestedContentType, content))
       }
     }
   }
}
