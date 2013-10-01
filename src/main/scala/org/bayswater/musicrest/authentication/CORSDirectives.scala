package org.bayswater.musicrest.authentication

import spray.http._
import spray.routing._
import spray.http.HttpHeaders._
/** This is a temporary CORS directive while we wait for
 *  the real one to find its way into a Spray release.
 */
trait CORSDirectives  { this: HttpService =>
  def respondWithCORSHeaders(origin: String) =
    respondWithHeaders(
      HttpHeaders.`Access-Control-Allow-Origin`(origin),
      HttpHeaders.`Access-Control-Allow-Credentials`(true)
    )

  def corsFilter(origin: String)(route: Route) =
    if (origin == "*")
      respondWithCORSHeaders("*")(route)
    else
      optionalHeaderValueByName("Origin") {
        case None => 
          route        
        case Some(clientOrigin) => {
          if (origin == clientOrigin)
            respondWithCORSHeaders(origin)(route)
          else {
            // Maybe, a Rejection will fit better
            complete(StatusCodes.Forbidden, "Invalid origin")
          }      
        }
      }
}
