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

package org.bayswater.musicrest.authentication

import com.typesafe.config.ConfigException
import scala.concurrent.{ ExecutionContext, Future, Promise }  
import scala.concurrent.ExecutionContext.Implicits.global
import spray.routing.authentication._
import spray.routing.authentication.BasicUserContext
import org.bayswater.musicrest.model.UserModel

/** an authenticator guarding access to particular URLs that checks credentials with the backend
 * 
 */

  object Backend {
 
    val UserAuthenticator = UserPassAuthenticator[BasicUserContext] { userPassOption ⇒ Future(userPassOption match {
        case Some(UserPass(user, pass)) => {
            try {
              // println("Authenticating: " + user + ":" + pass)
              if (UserModel().isValidUser(user, pass) ) Some(BasicUserContext(user)) else None
            } 
            catch {
              case _ : Throwable => None
            }
        }
        case _ => None
      })
    }

    val AdminAuthenticator = UserPassAuthenticator[BasicUserContext] { userPassOption => {
      val userFuture = UserAuthenticator(userPassOption)
      userFuture.map(someUser => someUser.filter(bu => bu.username == "administrator"))  
      }
    }
  }
 
 
