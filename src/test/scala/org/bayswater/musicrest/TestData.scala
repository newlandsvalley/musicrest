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
import java.net.URLEncoder

import scala.io.Source
import scalaz.Validation
import spray.http.FormData
import org.bayswater.musicrest.abc.{Abc, AbcSubmission}
import org.bayswater.musicrest.model.{TuneModel,User, UnregisteredUser}
import org.bayswater.musicrest.tools.GenreInsert

object TestData {
  
  
  val speedThePlough = """X:1
T:Speed The Plough
M:4/4
L:1/8
N:Simple version
Z:Steve Mansfield 1/2/2000
K:G
R:reel
GABc dedB | dedB dedB | c2ec B2dB | A2A2 A2 BA|
GABc dedB | dedB dedB | c2ec B2dB | A2A2 G4 ::
g2g2 g4 | g2fe dBGB | c2ec B2dB | A2A2 A4 |
g2g2 g4 | g2fe dBGB | c2ec B2dB | A2A2 G4 :|"""
    
  val noonLasses = """X: 1
T: Noon Lasses
T: East Clare, The
M: 4/4
L: 1/8
R: reel
K: Gmaj
E|G2 BG AcBA|GBAG GEDE|G2 BG AGAc|BAGE EDDE|
G2 BG AcBA|GBAG GEDE|G2 BG AGAc|BAGE ED D2||
BddB AGAc|BddB BAAc|BddB AGAc|BAGE ED D2|
BddB AGAc|BddB BAAc|BddB GFGB|ABGE ED D2||
dgbg agbg|dgbg ageg|dgbg ageg|dedc BG G2|
dgbg agbg|dgbg agef|gage dedB|BAGE ED Dd||
edBd edBd|edBA GEEd|edBd edBA|GAGE ED Dd|
edBd edBd|edBA GEEd|edBd ef g2|GAGE ED D||"""
    
   val frahers = """X:2
T:Fraher's Jig
R:jig
C: as played by John Wynne on "Pride of the West"
Z:transcribed by moritz
M:6/8
K:Dmix
|A3 GEA|DED ~G3|AEA GEA|~D3 DE/F/G|A2A GEA|DAF GAB|A2A GEA|~D3 D3|
AEA GEA|DED ~G3|AEA GEA|~D3 DE/F/G|A2A GEA|DAF GAB|AEA GEA|~D3 D3|
(3ABc A d2A|dcA AGE|GAB c2A|BGE EDD|ddA d2f|dcA AGE|~A3 GEA|~D3 D3|
(3ABc A dcA|dcA AGE|GAB c2B|BGE EDD|ddA f<fe|d2A AGE|AEA GEA|~D3 D3||
| AEA GEA|D2D GDD|ADD GEA|~D3 DE/F/G|A2A GEA|DAF GAB|c2A GEA|~D3 D3|
AEA GEA|DAD ~G3|A2A GEA|~D3 DE/F/G|A2A GEA|~D3 ~G3|AEA GEA|~D3 D3|
(3ABc A dcA|d2c AGE|GAB c2 z/ B/-|BGE EDD|dAA d2f|dcA AGE|~A3 GEA|~D3 D3|
(3ABc A dcA|d2c AGE|GAB cBA|BGE EDD|ddA =f2e|d2A AGE|A2A GEA|DFA d3|]"""
   
  val figForaKiss = """X: 1
T: A Fig For A Kiss
M: 9/8
L: 1/8
R: slip jig
K: Edor
|: G2B E2B BAG | F2A D2A AGF | G2B E2B BAG |
[1 B/c/dB AGF DEF :|2 B/c/dB AGF E3 ||
|: g2e g2e edB | f2d dcd fed |1 g2e g2e edB |
dBG GBd e2f :|2 gfe fed ecA | B/c/dB AGF E2F ||"""
    
val hälleforsnäs = """X:1
T:Polska fran Hälleforsnäs
C:Fredrik Willhelm Larsson
N:adapted from the ABC on http://www.folkwiki.se/Musik/365 - thanks!
M:3/4
L:1/16
K:Amin
R: Polska
|: E4 A4 B4 | c2Bc d2c2 B2A2 | E3A A2B2 c2d2 | e2fe d2ef e4 |
E4 A4 B4 | c2Bc d2c2 B2A2 | E3A A2B2 c2B2 |1 G2A2 A4 dcBA :| |2 G2A2 A8 | 
|: g2>f2 e2d2 e2c2 | c2Bc d2c2 B2A2 | E3A A2B2 c2d2 | e2fe d2ef e4 |
g2>f2 e2d2 e2c2 | c2Bc d2c2 B2A2 | E3A A2B2 c2B2  | G2A2 A8 :|"""
  
  
  def abcFor(content: String, submitter: String): Validation[String, Abc] = {
    val lines = Source.fromString(content).getLines
    val abcSubmission = AbcSubmission(lines, "irish", submitter)
    abcSubmission.validate        
  }
  
  def abcFor(content: String): Validation[String, Abc] = abcFor(content, "administrator")
  
  
  def newUser1FormData =
    FormData(Map("name" -> URLEncoder.encode("mike-mcgoldrick", "UTF-8"),
                "email" -> URLEncoder.encode("changeIt@gmail.com", "UTF-8"),
                "password" -> URLEncoder.encode("mcg0ldr1ck", "UTF-8"),
                "password2" -> URLEncoder.encode("mcg0ldr1ck", "UTF-8")))
                
  def newUser2FormData =
    FormData(Map("name" -> URLEncoder.encode("kevin-crawford", "UTF-8"),
                "email" -> URLEncoder.encode("changeIt@gmail.com", "UTF-8"),
                "password" -> URLEncoder.encode("crawf0rd", "UTF-8"),
                "password2" -> URLEncoder.encode("crawf0rd", "UTF-8")))                
                
  def newPasswordFormData =
    FormData(Map("password" -> URLEncoder.encode("passw0rd2", "UTF-8")))                
  
  def basicUsers = {
   val dbName = "tunedbtest"
   val collection = "user"
   val tuneModel = TuneModel()
   tuneModel.deleteUsers  
   val user1 = User("test user", "test.user@gmail.com", "passw0rd1")
   tuneModel.insertPreRegisteredUser(user1) 
   val user2 = User("untrustworthy user", "untrustworthy.usern@gmail.com", "password2")
   tuneModel.insertPreRegisteredUser(user2)       
   val user3 = User("administrator", "john.watson@gmx.co.uk", "adm1n1str80r")
   tuneModel.insertPreRegisteredUser(user3)    
  }  
 
 def unregisteredUser:Validation[String, UnregisteredUser] = {
   // mathias is an as yet unregistered user
   val tuneModel = TuneModel()
   val user3 = User("mathias", "mathias@gmail.com", "math1as")
   tuneModel.insertUser(user3)       
 }
 
 def basicGenres = {
   GenreInsert.insertAllGenres("tunedbtest")
 }
 
 def clearIrishTunes = {
   val dbName = "tunedbtest"
   val tuneModel = TuneModel()
   tuneModel.delete("irish")
 } 
 
}