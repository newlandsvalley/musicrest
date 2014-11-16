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

package org.bayswater.musicrest.abc

import org.specs2.mutable._
import scalaz._

import java.io._
import scala.io.Source
import org.bayswater.musicrest.TestData._

class AbcSpec extends Specification {

  "The ABC parser" should {
    "parse well formed ABC" in {  
        val v = getValidTune(speedThePlough)
        v.fold(e => failure("parse should succeed but failed: " + e), s => {
          s.name must_==  "Speed The Plough"
          s.safeFileName must_== ("speedtheploughreel")
          s.id must_== ("speed the plough-reel")
          s.timeSignature must_== (Some("4/4"))
          s.author must_== (Some("Steve Mansfield 1/2/2000"))
          s.key must_== (Some("Gmaj"))
          s.tuneType must_== ("reel") 
        })    
    }
    "Accept a tune with two titles" in {     
        val v = getValidTune(noonLasses)
        v.fold(e => failure("parse should succeed but failed: " + e), s => {
          s.name must_== ("Noon Lasses")
          s.timeSignature must_== (Some("4/4"))
          s.author must_== (Some("John Watson 12/11/2014"))
          s.key must_== (Some("Gmaj"))
          s.tuneType must_== ("reel")
        })
    }
    
    "Add a further title" in {      
        val v = getValidTune(noonLasses)
        v.fold(e => failure("Noon Lasses parse should succeed but failed: " + e), oldAbc => {
          val newAbc = oldAbc.addAlternativeTitle("Tommy Gunn's")
          newAbc.fold (
              e => failure("should be able to add a new title")
              ,
              s => {
                s.titles must_==  (List("Noon Lasses", "East Clare, The", "Tommy Gunn's"))
                s.abc.contains("T: Tommy Gunn's\n") must_==  (true)
              })
        })      
    }        
   "Disallow adding a non-Unicode title" in {      
        val v = getValidTune(noonLasses)
        v.fold(e => failure("Noon Lasses parse should succeed but failed: " + e), oldAbc => {
          val newAbc = oldAbc.addAlternativeTitle("""Polska fran H\\'alleforsn\\'as""")
          newAbc.fold (
              e => e must_== """Please use Unicode for all titles and don't use backslashes - you have submitted Polska fran H\\'alleforsn\\'as"""
              ,
              s => {
                failure("should not be able to add a non-Unicode title")
              })
        })      
    }        
    "Reject a tune with no title" in {      
        val v = getValidTune(badData.nameless)
        v.fold(e => e must_== ("No title (T header) present in abc"), s => failure("accepted title-less tune" ))
    }
    "Reject a tune with a title not in Unicoode" in {      
        val v = getValidTune(badData.notUnicode)
        v.fold(e => e must_== ("Please use Unicode for all titles and don't use backslashes - you have submitted Polska fran H\\'alleforsn\\'as"), s => failure("accepted tune with title not in Unicode" ))
    }
    "Reject a tune with no key signature" in {      
        val v = getValidTune(badData.keyless)
        v.fold(e => e must_== ("No key Signature present in abc"), s => failure("accepted key-less tune" ))
    }
    "Reject a tune with a bad key signature" in {      
        val v = getValidTune(badData.badkey)
        v.fold(e => e must_== ("Unrecognized key signature: Baol"), s => failure("accepted tune with unknown key signature" ))
    }
    "Reject a tune with a bad rhythm" in {      
        val v = getValidTune(badData.badrhythm)
        v.fold(e => e must_== ("foo is not a recognized rhythm for the irish genre"), s => failure("accepted tune with unknown rhythm" ))
    }
    "Suppress self references in tune sources" in {      
        // (frahers has a reference S:http://someServer/musicrest/genre/irish/frahers)
        val v = getValidTune(frahers)
        v.fold(e => failure("parse should succeed but failed: " + e), s => {
           s.source must_== (None)
        })        
    }
  }
  
 def getValidTune(content: String) = {
    val lines = Source.fromString(content).getLines
    val abcSubmission = AbcSubmission(lines, "irish", "administrator")
    abcSubmission.validate        
  }
}

object badData {   

  val nameless = """X: 1
M: 6/8
L: 1/8
R: jig
K: Gmaj
G3 GBd|BGD E3|DGB d2 d|edB def|
g3 ged|ege edB|dee edB|gdB A2 B:|
c3 cBA|Bdd d2 e|dBG GBd|edB AFD|
GBd gag|ege edB|dee edB|gdB A3:|"""
    
  val keyless = """X: 1
T: This tune has no key
M: 6/8
L: 1/8
R: jig
G3 GBd|BGD E3|DGB d2 d|edB def|
g3 ged|ege edB|dee edB|gdB A2 B:|
c3 cBA|Bdd d2 e|dBG GBd|edB AFD|
GBd gag|ege edB|dee edB|gdB A3:|"""
    
  val badkey = """X: 1
T: This tune has an unrecognized key
M: 6/8
L: 1/8
R: jig
K: Baol
G3 GBd|BGD E3|DGB d2 d|edB def|
g3 ged|ege edB|dee edB|gdB A2 B:|
c3 cBA|Bdd d2 e|dBG GBd|edB AFD|
GBd gag|ege edB|dee edB|gdB A3:|""" 
    
  val badrhythm = """X: 1
T: This tune has an unrecognized rhythm
M: 6/8
L: 1/8
R: foo
K: Gmaj
G3 GBd|BGD E3|DGB d2 d|edB def|
g3 ged|ege edB|dee edB|gdB A2 B:|
c3 cBA|Bdd d2 e|dBG GBd|edB AFD|
GBd gag|ege edB|dee edB|gdB A3:|"""
    
  val notUnicode =""" +X:1
T:Polska fran H\"alleforsn\"as
C:Fredrik Willhelm Larsson
N:adapted from the ABC on http://www.folkwiki.se/Musik/365 - thanks!
M:3/4
L:1/16
K:Dm
R: Polska
c2>B2 A2G2 A2F2 | F2EF G2F2 E2D2 | A,2D2 D2E2 F2G2 | A2BA G2AB A4 |
c2>B2 A2G2 A2F2 | F2EF G2F2 E2D2 | A,2D2 D2E2 F2ED | C4 D4 D4 ::
A,4 D4 E4 | F2EF G2F2 E2D2 | A,2D2 D2E2 F2G2 | A2BA G2AB A4 |
A,4 D4 E4 | F2EF G2F2 E2D2 | A,2D2 D2E2 F2ED | C4 D4 D4 :|"""

}
