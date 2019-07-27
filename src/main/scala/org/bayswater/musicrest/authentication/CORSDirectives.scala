package org.bayswater.musicrest.authentication

import spray.http._
import spray.routing._
import spray.http.HttpHeaders._
import spray.http.HttpMethods._
/** This is a temporary CORS directive while we wait for
 *  the real one to find its way into a Spray release.
 */
trait CORSDirectives  { this: HttpService =>
  private def respondWithCORSHeaders(origin: String) =
    respondWithHeaders(
      HttpHeaders.`Access-Control-Allow-Origin`(SomeOrigins(List(origin))),
      HttpHeaders.`Access-Control-Allow-Credentials`(true),
      HttpHeaders.`Access-Control-Expose-Headers`(List ("Musicrest-Pagination"))
    )
  private def respondWithCORSHeadersAllOrigins =
    respondWithHeaders(
      HttpHeaders.`Access-Control-Allow-Origin`(AllOrigins),
      HttpHeaders.`Access-Control-Allow-Credentials`(true),
      HttpHeaders.`Access-Control-Expose-Headers`(List ("Musicrest-Pagination"))
    )
  private def respondWithCORSOptionsHeaders(origin: String) =
    respondWithHeaders(
      HttpHeaders.`Access-Control-Allow-Origin`(SomeOrigins(List(origin))),
      HttpHeaders.`Access-Control-Allow-Methods`(List (POST, GET, OPTIONS, DELETE)),
      HttpHeaders.`Access-Control-Allow-Headers`(List ("Authorization")),
      HttpHeaders.`Access-Control-Max-Age`(86400)
    )
  private def respondWithCORSOptionsHeadersAllOrigins =
    respondWithHeaders(
      HttpHeaders.`Access-Control-Allow-Origin`(AllOrigins),
      HttpHeaders.`Access-Control-Allow-Methods`(List (POST, GET, OPTIONS, DELETE)),
      HttpHeaders.`Access-Control-Allow-Headers`(List ("Authorization")),
      HttpHeaders.`Access-Control-Max-Age`(86400)
    )

  def corsFilter(origins: List[String])(route: Route) =
    if (origins.contains("*"))
      respondWithCORSHeadersAllOrigins(route)
    else
      optionalHeaderValueByName("Origin") {
        case None =>
          route
        case Some(clientOrigin) => {
          if (origins.contains(clientOrigin))
            respondWithCORSHeaders(clientOrigin)(route)
          else {
            // Maybe, a Rejection will fit better
            complete(StatusCodes.Forbidden, "Invalid origin")
          }
        }
      }

  def corsOptionsFilter(origins: List[String])(route: Route) =
    if (origins.contains("*"))
      respondWithCORSOptionsHeadersAllOrigins(route)
    else
      optionalHeaderValueByName("Origin") {
        case None =>
          route
        case Some(clientOrigin) => {
          if (origins.contains(clientOrigin))
            respondWithCORSOptionsHeaders(clientOrigin)(route)
          else {
            // Maybe, a Rejection will fit better
            complete(StatusCodes.Forbidden, "Invalid origin")
          }
        }
      }

  /** this is similar to CORS in that a browser will now ignore a download attribute within a request for
   *  a resource from an anchor tag if the URL represents a cross-origin resource.  We want to produce such a
   *  direct link to the (cross-origin) backend for file downloads.  One way to circumvent this is for the
   *  backend to indicate the download file name by means of the content-disposition header
   */
  def suggestDownloadFileName(filetype: String, tune: String) =
    respondWithHeaders (
      HttpHeaders.`Content-Disposition`("attachment", Map("filename" -> s"$tune.$filetype"))
    )
}
