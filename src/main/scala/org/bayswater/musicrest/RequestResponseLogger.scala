package org.bayswater.musicrest

import akka.event.Logging._
import spray.routing._
import spray.http._
import directives.DebuggingDirectives._
import directives.LogEntry

object RequestResponseLogger {
   
  /** simple logger which mostly logs the content types of both request and response*/ 
  val rrlogger: HttpRequest =>  Any => Option[LogEntry] =
    req => res => {
      val accept = req.headers.filter(h => h.name == ("Accept")).headOption.getOrElse(None)
      val incoming = s"method: ${req.method}, accept: $accept, uri: ${req.uri}"  
      val outgoing = res match {
        case r: HttpResponse => { 
          r.entity match { 
            case b: HttpBody =>  s"status: ${r.status} content type: ${b.contentType.mediaType}"
            case _ => s"status: ${r.status}" 
          }          
        }
        case Rejected(rejections) => s"Rejection: ${rejections.toString}"
        case Confirmed(messagePart, sentAck) => "chunk response"
        case x => s"Unexpected response: ${x.toString}"
      }
      // suppress log of chunking messsages
      if ("chunk response" == outgoing)
        None
      else
        Some(LogEntry("REQ " + incoming + "\nRESP " + outgoing, DebugLevel))
    }
}