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
import scalaz.syntax.validation._
import spray.http.FormData
import org.bayswater.musicrest.abc.{Abc, AbcSubmission}
import org.bayswater.musicrest.model.{TuneModel, UserModel, User, UnregisteredUser}
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
Z: John Watson 12/11/2014
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
S:http://someServer/musicrest/genre/irish/frahers
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

 val tempest = """X: 1
T: Tempest
R: reel
M: 4/4
L: 1/8
K: Cmaj
|:d2 cA GE E2|DEcE dEcE|d2 cA GE E2|DEAE GEDE|
d2 cA GE E2|DEcE dEcE|DEFA GEcE|EDcG ED D2:|
|:d2 ed cAGA|Addc AGEG|d2 ed cdef|edce d3 z|
efdf edcA|dcAG AGEG|DEFA GEcE|EDcG ED D2:|"""

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

val vardet = """X: 1
T: Var det du eller var det jag?
O: Ryd, Öland
S: Efter Anders Johan Eriksson
R: Waltz
M: 3/4
K: Ador
L: 1/8
|:AB | (c2 c)dBc | A2>B2cd | e2 g2 f2 | e4 :|
|: e2 | e2 c2 ec | c2 ec eg | f2 d2 (fd) |
d2 fd fg | (e2>d2)cB | A4 c2 | B2 G2 G2 | A4 :|"""



val idreSchottis = """X:8748
T:Schottis fran Idre
A:Dalarna
C:e. Storbo-Jöns
F:http://richardrobinson.tunebook.org.uk/tune/3056
L:1/16
M:2/4
O:Sweden
Q:70
R:Schottis
K:Ddor
aage f2ed | ^c2A2 A4 | AB^cd e2ec | d^cdf a4 |\
aage f2ed | ^c2A2 A4 | AB^cd efec | d2d2 d4 ::
f2fg f2ed | e2a2 a2ge | f2fg f2ed | efe^c A4 |\
f2fg f2ed | e2a2 a2ge | f2df e2A^c | d2d2 d4 ::
d2d2 d^ced | ^c2A2 A2ag | f2ff f2gf | f2e^c A3A |\
Add2 d^ced | ^c2A2 A2ag | f2df e2A^c | d2d2 d4 :|"""

  def abcFor(genre: String, content: String, submitter: String): Validation[String, Abc] = {
    val lines = Source.fromString(content).getLines
    val abcSubmission = AbcSubmission(lines, genre, submitter)
    abcSubmission.validate
  }

  def abcFor(genre: String, content: String): Validation[String, Abc] = abcFor(genre, content, "administrator")


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
   println("basicUsers")
   val dbName = "tunedbtest"
   val collection = "user"
   val userModel = UserModel()
   userModel.deleteUsers
   val user1 = User("test user", "test.user@gmail.com", "passw0rd1")
   userModel.insertPreRegisteredUser(user1)
   val user2 = User("untrustworthy user", "untrustworthy.usern@gmail.com", "password2")
   userModel.insertPreRegisteredUser(user2)
   val user3 = User("administrator", "john.watson@gmx.co.uk", "adm1n1str80r")
   userModel.insertPreRegisteredUser(user3)
  }

 def unregisteredUser:Validation[String, UnregisteredUser] = {
   // mathias is an as yet unregistered user
   val userModel = UserModel()
   val user3 = User("mathias", "mathias@gmail.com", "math1as")
   userModel.insertUser(user3)
 }

 def basicGenres = {
   println("basicGenres")
   GenreInsert.insertAllGenres("localhost", "musicrest", "password", "tunedbtest")
 }

 def clearIrishTunes = {
   val dbName = "tunedbtest"
   val tuneModel = TuneModel()
   tuneModel.delete("irish")
 }

  def commentableTune(i: Int) = s"""X: 1
T: tune${i}
M: 4/4
L: 1/8
R: reel
K: Edor
|: ABC ||"""

  def insertCommentableTunes = {
    val dbName = "tunedbtest"
    val tuneModel = TuneModel()
    val genre = "irish"
    tuneModel.delete(genre)
    for (i <- 0 to 15) {
      val abc = abcFor(genre, commentableTune(i))
      abc.fold(e => println("unexpected error in test data: " + e),
               s => s.upsert("irish"))
    }
  }

  /* wrap in a validation  so we can use in for loops */
  def delay(tics: Int): Validation[String, String] =
    try {
      Thread.sleep(tics)
      (s"delayed for ${tics} ms").success
    }
    catch {
      case t: Throwable  =>  "delay interrupted".failure[String]
    }
    finally {
    }

}
