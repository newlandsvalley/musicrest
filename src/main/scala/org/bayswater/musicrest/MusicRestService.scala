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

import java.net.URLDecoder
import java.io.File
import akka.actor.Actor
import spray.routing._
import directives.DebuggingDirectives._
import spray.http._
import spray.http.HttpHeaders._
import MediaTypes._
import spray.routing.authentication.BasicAuth
import spray.util.{LoggingContext, SprayActorLogging}

import scala.concurrent.{ ExecutionContext, Future, Promise }
import scala.concurrent.ExecutionContext.Implicits.global

import scalaz.Validation
import scalaz.syntax.validation._
import scalaz.{ \/, -\/, \/- }

import org.bayswater.musicrest.abc._
import org.bayswater.musicrest.abc.Tune._
import org.bayswater.musicrest.abc.AbcPost._
import org.bayswater.musicrest.abc.AlternativeTitlePost._
import org.bayswater.musicrest.model.{TuneModel, Comment, Comments,User,UserRef}
import org.bayswater.musicrest.cache.Cache._
import org.bayswater.musicrest.tools.Email
import org.bayswater.musicrest.authentication.Backend._
import org.bayswater.musicrest.authentication.CORSDirectives
import org.bayswater.musicrest.RequestResponseLogger._


// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class MusicServiceActor extends Actor with MusicRestService {

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = runRoute(musicRestRoute)
}

/** This is (I hope) a temporary fix.  At the moment all processing errors are saved as validation
 *  failures, but then, disappointingly, converted to MusicRestExceptions.  If we do nothing,
 *  the default exception handler just translates them into bland 500 or 400 errors with no explanatory
 *  error text.  So instead, we wrap our routes in our own exception handler which builds a more
 *  comprehensible HTTP status message.
 *
 *  It is particularly awkward because we need to protect with CORS headers
 *  even though we don't know the route.  We'll allow any JS client to
 *  see the error page.
 */
object MusicRestService {
  import spray.http.StatusCodes.BadRequest

  import directives.RespondWithDirectives._

  case class MusicRestException(message: String) extends RuntimeException(message, null)

  private def respondWithCORSHeaders =
    respondWithHeaders(
      HttpHeaders.`Access-Control-Allow-Origin`(AllOrigins),
      HttpHeaders.`Access-Control-Allow-Credentials`(true)
    )

  implicit def myExceptionHandler(implicit log: LoggingContext) =
    ExceptionHandler {
      case e: MusicRestException =>
        respondWithCORSHeaders {
          ctx =>
            log.info(s"Error processing request: ${e.getMessage} ${ctx.request}")
            // frame the message so we can distinguish our text from HTTPP's
            ctx.complete(BadRequest, e.getMessage)
        }
  }
}


/** MusicRestService:
 *
 *  musicRestRoute = tuneRoute ~ commentsRoute ~ userRoute
 *
 */
trait MusicRestService extends HttpService with CORSDirectives {

  import MusicRestService.myExceptionHandler

  val supportedGenres = SupportedGenres
  val logger = implicitly[LoggingContext]
  println(s"log debug?: ${logger.isDebugEnabled} info?: ${logger.isInfoEnabled} ")

  val begin = {
    establishGenres
  }


  /* this trait defines our service behaviour independently from the service actor
   *
   * a route that deal with genres and tunes in their various guises
   *
   * overall route is musicRestRoute = tuneRoute ~ commentsRoute ~ userRoute
   */
  val tuneRoute =
    pathPrefix("musicrest") {
      pathEndOrSingleSlash {
        get {
            // logger.info("welcome")
            complete (Welcome())
        }
      }
    } ~
    path("musicrest" / "config" ) {
      get {
          authenticate(BasicAuth(AdminAuthenticator, "musicrest")) { user =>
             _.complete("config: scriptDir: " + MusicRestSettings.scriptDir +
                              " abcDirCore: " + MusicRestSettings.abcDirCore +
                              " pdfDirCore: " + MusicRestSettings.pdfDirCore +
                              " abcDirPlay: " + MusicRestSettings.abcDirPlay +
                              " wavDirPlay: " + MusicRestSettings.wavDirPlay +
                              " dbName: " + MusicRestSettings.dbName    +
                              " genres: " + supportedGenres.genres.toString)
          }
        }
    } ~
    // list of music genres
    path("musicrest" / "genre") {
      get { ctx => ctx.complete(GenreList()) }
    } ~
    pathPrefix("musicrest" / "genre" ) {
      path(Segment ) { genre =>
        get { ctx => ctx.complete(RhythmList(genre)) }
      } ~
      path(Segment / "exists" ) { genre =>
        // return true if the genre exists (is supported)
        get {
          respondWithMediaType(`text/plain`) {
            complete{
              val exists = SupportedGenres.isGenre(genre)
              exists.toString
              }
          }
        }
      } ~
      // within genre
      path(Segment / "tune"  ) { genre =>
        post {
          authenticate(BasicAuth(UserAuthenticator, "musicrest")) { user =>
            entity(as[AbcPost]) { post =>
              corsFilter(MusicRestSettings.corsOrigins) {
                complete {
                  val abcSubmission = AbcSubmission(post.notes.lines, genre, user.username)
                  val validAbc:Validation[String, Abc] = abcSubmission.validate
                  // response:\/[String, TuneRef] =
                  val response =
                    for {
                      g <- GenreList.validate(genre).disjunction
                      abc <- validAbc.disjunction
                      // we will try to transcode immediately to png which might cause further errors
                      t <- abc.to(`image/png`).disjunction
                      success <- abc.upsert(genre).disjunction
                    } yield success
                  response.validation
                }
              }
            }
          }
        } ~
        // list of tunes within the genre
        parameters('sort ? "alpha", 'page ? 1, 'size ? MusicRestSettings.defaultPageSize) { (sort, page, size) =>
          get {
            val tuneList = TuneList(genre, sort, page, size)
            val totalPages = (tuneList.totalResults + size - 1) / size
            corsFilter(MusicRestSettings.corsOrigins) {
              respondWithHeader(paginationHeader(page, totalPages)) {
                ctx => ctx.complete(tuneList)
                }
              }
            }
        } ~
        options {
          corsOptionsFilter(MusicRestSettings.corsOrigins) {
            _.complete {
              "options".success
            }
          }
        }
      } ~
      path(Segment / "tune" / Segment) { (genre, tuneEncoded) =>
        options {
          corsOptionsFilter(MusicRestSettings.corsOrigins) {
            _.complete {
              "options".success
            }
          }
        } ~
        // get a tune (in whatever format)
        get {
          val tune = java.net.URLDecoder.decode(tuneEncoded, "UTF-8")
          // logger.info(s"get - tune name encoded: ${tuneEncoded} decoded: ${tune}")
          corsFilter(MusicRestSettings.corsOrigins) {
            complete(Tune(genre, tune) )
            }
        } ~
        delete {
          authenticate(BasicAuth(UserAuthenticator, "musicrest")) { user =>
            val tune = java.net.URLDecoder.decode(tuneEncoded, "UTF-8")
            val submitter = TuneModel().getSubmitter(genre, tune)
            logger.info(s"original submitter of $tune was $submitter - delete requester: ${user.username}")
            // allow the administrator or the original submitter to delete the tune
            val authorized = (submitter, user.username) match {
              case (Some(_), "administrator") => true
              case (Some(s), u) => s == u
              case _ => false
            }
            corsFilter(MusicRestSettings.corsOrigins) {
              if (authorized) complete {
                // delete from the file system cache
                val dir = new File(MusicRestSettings.transcodeCacheDir)
                clearTuneFromCache(dir, tune)
                // delete all related comments
                Comments.deleteComments(genre, tune)
                // delete from the database
                val result = TuneModel().delete(genre, tune)
                result
              }
              else {
                // we'll use the horrible exception mechanism for the time being to get
                // a proper HTTP response
                val message =
                  if (submitter.isDefined) {
                    s"You can only delete tunes that you originally submitted"
                  }
                  else {
                    s"No such tune: $tune"
                  }
                throw new MusicRestService.MusicRestException(message)
              }
            }
          }
        }
      } ~
      path(Segment / "tune" / Segment / "exists" ) { (genre, tuneEncoded) =>
        // return true if the tune exists in the database
        get {
          respondWithMediaType(`text/plain`) {
            complete {
              val tuneNotes = java.net.URLDecoder.decode(tuneEncoded, "UTF-8")
              Tune(genre, tuneNotes).exists
            }
          }
        }
      } ~
      path(Segment / "tune" / Segment / "abc" ) { (genre, tuneEncoded) =>
        // get a tune (in abc defined by abc.vnd (plain text) format)
        // as for binary types, suggest a file name for use with download attributes
        get {
          respondWithMediaType(Tune.AbcType) {
            val tuneName = java.net.URLDecoder.decode(tuneEncoded, "UTF-8")
            val tune = Tune(genre, tuneName)
            suggestDownloadFileName("abc", tune.safeFileName) {
              complete{
                tune.asAbc
              }
            }
          }
        } ~
        post {
          // add an alternative tune title
          authenticate(BasicAuth(UserAuthenticator, "musicrest")) { user =>
            entity(as[AlternativeTitlePost]) { post =>
              val tune = java.net.URLDecoder.decode(tuneEncoded, "UTF-8")
              complete {
                Abc.addAlternativeTitle(genre, tune, post.title)
              }
            }
          }
        }
      } ~
      path(Segment / "tune" / Segment / "html" ) { (genre, tuneEncoded) =>
        // get a tune (in abc represented in html format)
        get {
          respondWithMediaType(`text/html`) {
            val tuneName = java.net.URLDecoder.decode(tuneEncoded, "UTF-8")
            complete(Tune(genre, tuneName).asHtml )
          }
        }
      } ~
      path(Segment / "tune" / Segment / "json" ) { (genre, tuneEncoded) =>
        // get a tune (in abc represented in json format)
        get {
          respondWithMediaType(`application/json`) {
            corsFilter(MusicRestSettings.corsOrigins) {
              val tuneName = java.net.URLDecoder.decode(tuneEncoded, "UTF-8")
              complete(Tune(genre, tuneName).asJson )
            }
          }
        }
      } ~
      path(Segment / "tune" / Segment / "wav" ) { (genre, tuneEncoded) =>
        parameters ('instrument ? "piano", 'transpose ? 0, 'tempo ? "120") { (instrument, transpose, tempo) =>
          get {
            val tune = java.net.URLDecoder.decode(tuneEncoded, "UTF-8")
            // System.out.println(s"got wav request for $tune instrument %instrument, transpose $transpose, tempo $tempo")
            val validTune = Tune.tuneToWav(Tune(genre, tune), instrument, transpose, tempo)
            validTune.fold (
               e => _.complete(e),
               s => getFromFile(s)
            )
          }
        }
      }  ~
      /**  Get a temporary tune in png format (resulting from a try transcode */
      path(Segment / "tune" / Segment / "temporary" / "png"  ) { (genre, tuneEncoded) =>
        // get a tune (in abc represented in html format)
        get {
          respondWithMediaType(`image/png`) {
            val tuneName = java.net.URLDecoder.decode(tuneEncoded, "UTF-8")
            // System.out.println("got request for temporary tune image for " + tuneName)
            _.complete {
              val futureImage: Future[Validation[String, BinaryImage]] =  Tune(genre, tuneName).asFutureTemporaryImage
              futureImage
            }
          }
        }
      }  ~
      /** Get a tune in one of the supported binary file type formats (ps, pdf, midi )
       *  This is an alternative URL which sidesteps content negotiation and just
       *  returns the MIME type implied by the file extension (or an error if it's not supported)
       *
       *
       *  Anchor tag download attribute in prospective clients:  we offer a suggested file name by means of
       *  the content-disposition header
       */
      path(Segment / "tune" / Segment / Segment ) { (genre, tuneEncoded, fileType) =>
        get {
          val contentTypeOpt = getContentTypeFromFileType(fileType:String)
          if (contentTypeOpt.isDefined) {
            respondWithMediaType(contentTypeOpt.get.mediaType) {
              val tuneName = java.net.URLDecoder.decode(tuneEncoded, "UTF-8")
              val tune = Tune(genre, tuneName)
              suggestDownloadFileName(fileType, tune.safeFileName) {
                corsFilter(MusicRestSettings.corsOrigins) {
                   _.complete {
                     // val futureBin:Future[Validation[String, BinaryImage]] = Tune(genre, tune).asFutureBinary(fileType)
                     val futureBin = tune.asFutureBinary(contentTypeOpt.get)
                     futureBin
                  }
                }
              }
            }
          }
          else {
            // now we have comments, we can't fail here because the URLS of this structure interfere
            // i.e the rejection should allow for an identical URL where the last Segment is 'comments'
            // failWith(new Exception("Unrecognized file extension " + fileType))
            reject()
          }
        }
      } ~
      path(Segment / "search"  ) { genre =>
        parameters ('page ? "1", 'size ? MusicRestSettings.defaultPageSize.toString, 'sort ? "alpha") { (pageStr, sizeStr, sort) =>
          get {
            corsFilter(MusicRestSettings.corsOrigins) {  ctx =>
              // val searchParams = Map("page" -> pageStr, "size" -> sizeStr, "sort" -> sort)
              val queryParams = ctx.request.uri.query.toMultiMap
              // Spray now provides a MultiMap not a Map which we need to flatten
              val searchParams = queryParams mapValues {x => x.head }
              //System.out.println(s"search params $searchParams")
              val page = pageStr.toInt
              val size = sizeStr.toInt
              // System.out.println("request for page: " + page)
              ctx.complete {
                val tuneList = TuneList(genre, searchParams, sort, page, size)
                val totalPages = (tuneList.totalResults + size - 1) / size
                val headers = List(paginationHeader(page, totalPages))
                (200, headers, tuneList)
                }
              }
            }
          }
      } ~
      // one-off transcoding service
      path(Segment / "transcode"  ) { genre =>
        authenticate(BasicAuth(UserAuthenticator, "musicrest")) { user =>
          post {
            entity(as[AbcPost]) { post =>
              complete {
                val abcSubmission = AbcSubmission(post.notes.lines, genre, user.username)
                val validAbc:Validation[String, Abc] = abcSubmission.validate
                // response:\/[String, TuneRef]
                val response =
                  for {
                     g <- GenreList.validate(genre).disjunction
                     abc <- validAbc.disjunction
                     // we will try to transcode immediately to png which might cause further errors
                     t <- abc.createTemporaryImageFile.disjunction
                     success <- abc.validTuneRef(genre).disjunction
                  } yield success
                response.validation
              }
            }
          }
        }
      }          // end of path prefix musicrest/genre
    }



  val commentsRoute =
    pathPrefix("musicrest" / "genre" ) {
      // we support a URL for deleting all comments within a genre but expect it will
      // only really be used in testing or in initial setup
      path(Segment / "comments" ) { genre =>
        delete {
          authenticate(BasicAuth(AdminAuthenticator, "musicrest")) { user =>
            complete {
              Comments.deleteAllComments(genre)
              }
          }
        }
      } ~
      path(Segment / "tune" / Segment / "comments" ) { (genre, tuneEncoded) => {
        val tuneId = java.net.URLDecoder.decode(tuneEncoded, "UTF-8")
        // return all comments associated with the tune
        get {
          corsFilter(MusicRestSettings.corsOrigins) {
            complete {
              val commentsSeq = Comments.getComments(genre, tuneId)
              Comments(commentsSeq)
            }
          }
        } ~
        post {
          authenticate(BasicAuth(UserAuthenticator, "musicrest")) { user =>
            /* Post is used both for posting the original comment and editing a comment.
             * In each case, submitter is the original submitter of the comment
             */
            post {
              formFields('user, 'timestamp, 'subject, 'text) { (submitter, timestamp, subject, text) =>  {
              val authorized = isOwnerOrAdministrator(submitter, user.username)

              corsFilter(MusicRestSettings.corsOrigins) {
                if (authorized) {
                  complete {
                    val comment = Comment(submitter, timestamp, subject, text)
                    Comments.insertComment(genre, tuneId, comment)
                    }
                }
                else {
                  println(s"user ${user.username} not authorized to insert comment owned by ${submitter} for ${tuneId}")
                  reject(AuthorizationFailedRejection)
                }
              }

              }  }
            }
          }
        } ~
        options {
          corsOptionsFilter(MusicRestSettings.corsOrigins) {
            _.complete {
              "options".success
            }
          }
        }
      } } ~
      path(Segment / "tune" / Segment / "comment" / Segment / Segment ) { (genre, tuneEncoded, submitterEncoded, timestamp) =>  {
        val tuneId = java.net.URLDecoder.decode(tuneEncoded, "UTF-8")
        val submitter = java.net.URLDecoder.decode(submitterEncoded, "UTF-8")
        /* CORS options */
        options {
          corsOptionsFilter(MusicRestSettings.corsOrigins) {
            _.complete {
              "options".success
            }
          }
        } ~
        /** Get an individual comment  */
        get {
          corsFilter(MusicRestSettings.corsOrigins) {
            complete {
              Comments.getComment(genre, tuneId, submitter, timestamp)
            }
          }
        } ~
        delete {
          authenticate(BasicAuth(UserAuthenticator, "musicrest")) { user =>   {

           val authorized = isOwnerOrAdministrator(submitter, user.username)

           corsFilter(MusicRestSettings.corsOrigins) {
              if (authorized) {
                respondWithMediaType(`text/plain`) {
                  complete {
                    Comments.deleteComment(genre, tuneId, submitter, timestamp)
                  }
                }
              }
              else  {
                reject(AuthorizationFailedRejection)
              }
            }  // end of cors filter
          } }
        } }
      }          // end of comments paths
    }



  /* route that deal with user maintenance */
  val userRoute =
    pathPrefix("musicrest") {
      path("user") {
       post {
          /* create new user.  We will allow any user to submit his details, but if we detect that the administrator
           * is the currently logged-in user, we assume he is creating a user of behalf of someone else
           * and so we set up a pre-registered user
           */
          formFields('name, 'email, 'password, 'password2, 'refererurl?) { (name, email, password, password2, refererUrl) =>  {
            userName  { userOpt => {
               // val vu:Validation[String, String]
               val doRegister = userOpt match {
                 case Some("administrator") => true
                 case _ => false
               }
               println(s"optional user is $userOpt is this the administrator $doRegister")
               corsFilter(MusicRestSettings.corsOrigins) {
                 complete {
                   val vu  = for {
                     n <- User.checkName(URLDecoder.decode(name, "UTF-8"))
                     e <- User.checkEmail(URLDecoder.decode(email, "UTF-8"))
                     p <- User.checkPassword(URLDecoder.decode(password, "UTF-8"), URLDecoder.decode(password2, "UTF-8"))
                     _ <- User.checkUnique(URLDecoder.decode(name, "UTF-8"))
                     u <- User(n,e,p).insert(doRegister).disjunction
                     /* New behaviour at 1.1.4.
                      * we'll send a different email depending on whether or not the user is pre-registered or
                      * whether the referring application provides us with a base URL for the registration link.
                      *
                      * At the moment, None means that there is no referer url to use - we'll use the musicrest one
                      * otherwise for Some(url) we use a confirmation email with a url to the reference we're supplied with
                      *
                      */
                     e <- Email.sendRegistrationMessage(u, doRegister, refererUrl).disjunction
                     } yield u
                   vu.validation
                   }
                 }
               }
             }
            }
          }
        } ~
        // list of users
        parameters('page ? 1, 'size ? MusicRestSettings.defaultPageSize) { (page, size) =>
          get {
            authenticate(BasicAuth(AdminAuthenticator, "musicrest")) { user =>
              val userList = UserList(page, size)
              val totalPages = (userList.totalResults + size - 1) / size
              respondWithHeader(paginationHeader(page, totalPages)) {
                corsFilter(MusicRestSettings.corsOrigins) {
                  ctx => ctx.complete(userList)
                }
              }
            }
          }
        }~
        options {
          corsOptionsFilter(MusicRestSettings.corsOrigins) {
            _.complete {
              "options".success
            }
          }
        }
      }~
      // validate a user (i.e. after he's responded to an email)
      path ("user" / "validate" / Segment ) { uuid =>
        get {
          ctx => {
            // debugRequestHeaders(ctx)
            ctx.complete {
              User.validate(uuid)
            }
          }
        }
      }~
      // reset the password for a user
      path("user" / "password" / "reset") {
        authenticate(BasicAuth(UserAuthenticator, "musicrest")) { user =>
          post {
            formFields('password) { password =>
              complete {
                User.alterPassword(user.username, password)
              }
            }
          }
        }
      }~
      // resend the password for a user
      path("user" / "password" / "resend") {
        post {
          formFields('name) { (name) =>
            complete {
              val vun =
                for {
                  ur <- User.get(name).disjunction
                  e <- Email.sendPasswordMessage(ur).disjunction
                } yield ur.name
                vun.validation
             }
           }
         }
      } ~
      // check a user login
      path ("user" / "check" ) {
        get {
          corsFilter(MusicRestSettings.corsOrigins) {
            authenticate(BasicAuth(UserAuthenticator, "musicrest")) { user =>
              _.complete("user is valid")
            }
          }
        } ~
        options {
          corsOptionsFilter(MusicRestSettings.corsOrigins) {
            _.complete {
              "options".success
            }
          }
        }
      } ~
      // some user maintenance functions
      // delete user
      path ("user" / Segment ) {  uname =>
        delete {
          authenticate(BasicAuth(AdminAuthenticator, "musicrest")) { user =>
            val name = java.net.URLDecoder.decode(uname, "UTF-8")
            complete {
              User.deleteUser(name)
            }
          }
        }
      }
    }



  /* overall route */
  val musicRestRoute =
    handleExceptions(myExceptionHandler) {
      logRequestResponse(rrlogger) {
        tuneRoute ~ commentsRoute ~ userRoute
      }
  }

  def paginationHeader(page: Int, totalPages: Long) : HttpHeaders.RawHeader =
       HttpHeaders.RawHeader("Musicrest-Pagination", "[" + page + " of " + totalPages + "]")

  /** return the content type from the file type (taken originally from the request URL) */
  private def getContentTypeFromFileType(fileType:String): Option[ContentType] = {
    val supportedTypes: Map[String, ContentType] = Map( "png" -> ContentType(`image/png`),
                                                        "pdf" -> ContentType(`application/pdf`),
                                                        "midi" -> ContentType(`audio/midi`),
                                                        "ps" -> ContentType(`application/postscript`) )
     supportedTypes.get(fileType)
  }

  def debugMessage(text: String) =
            <html>
              <body>
                <h1>{text}</h1>
              </body>
            </html>

  /** establish the genres and their rhythms */
  def establishGenres(implicit log: LoggingContext) =  {
    supportedGenres.createGenreSubdirectories()
    log.info(s"genres: ${supportedGenres.rhythmMap}")
  }

  /* return true if the logged-in user is the administrator or the owner of a resource */
  def isOwnerOrAdministrator(owner: String, user : String): Boolean =
    (owner, user) match {
       case (_, "administrator") => true
       case (o, u) => o == u
       case _ => false
  }


  /** directive for extracting the logged in user name */
  val userName = optionalHeaderValue {
      case Authorization(BasicHttpCredentials(user, _)) =>  {
        logger.info(s"new user: $user is the logged in user")
        Some(user)
      }
      case _ => {
        logger.info("new user: no user logged in")
        None
      }
  }





}
