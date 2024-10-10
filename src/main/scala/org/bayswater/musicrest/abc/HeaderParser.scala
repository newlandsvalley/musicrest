/*
 * Copyright (C) 2011-2024 org.bayswater
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
import scala.util.parsing.combinator._

/*
 ^ This is an ABC header parser which can potentially be expanded in the future so as to to validate
 * any header but at the moment is just used to validate the 'K' key signature header.
 *
 * When this is called, an attempt has already been made to normalise the key signature
 * for example Am => AMinor, A => AMajor etc. So there should already be a mode of some kind in place
 * where the key signature is originally in shortened form.
 */
class HeaderParser extends JavaTokenParsers {

    def keySig: Parser[String] = tonic~accidental~mode ^^
      { case t~a~m => t + a + m  }

    def tonic: Parser[String] = """[A-G]""".r | failure ("no tonic recognized in key signature")

    def accidental: Parser[String] = opt ("#" | "b") ^^
      { 
        case Some (a) => a 
        case None => ""
      }

    def mode: Parser[String] = 
        major | minor | ionian | dorian | phrygian | lydian | mixolydian | aeolian | locrian | failure ("no node recognized in key signature")

    def major: Parser[String] = """[M|m][A|a][J|j][A-Za-z]*""".r

    def minor: Parser[String] = """[M|m][I,i][N,n][A-Za-z]*""".r

    def ionian: Parser[String] = """[I|i][O|o][N|n][A-Za-z]*""".r

    def dorian: Parser[String] = """[D|d][O|o][R|r][A-Za-z]*""".r

    def phrygian: Parser[String] = """[P|p][H|h][R|r][A-Za-z]*""".r

    def lydian: Parser[String] = """[L|l][Y|y][D|d][A-Za-z]*""".r

    def mixolydian: Parser[String] = """[M|m][I|i][X|x][A-Za-z]*""".r

    def aeolian: Parser[String] = """[A|a][E|e][O|o][A-Za-z]*""".r

    def locrian: Parser[String] = """[L|l][O|o][C|c][A-Za-z]*""".r

}




