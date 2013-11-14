package org.bayswater.musicrest.model

import net.liftweb.json._
import spray.http._
import spray.httpx.marshalling._
import scalaz.Validation

case class Comments (comments: Seq[Comment]) {
  
  def toJSON: String = {
    val jcomments = for (c <- comments) yield {
      c.toJSON
    }
    val json = "{" +  """"comment" :""" + "[ " + jcomments.mkString(",\n") +"] " +  "}" 
    json
  }
  
  def toXML: String = {
      val jvalue = JsonParser.parse(toJSON)
      "<comments>" + Xml.toXml(jvalue).toString + "</comments>"
  }  
  
  /* supports json and xml at the moment */
  def to(mediaType:MediaType): String = {
      val formatExtension:String = mediaType.subType
      formatExtension match {
          case "json" => toJSON
          case "xml" =>  toXML
          case _ => "Unsupported media type: " + formatExtension
        }
    }            
}

object Comments {
  
  implicit val commentsMarshaller = {    

     val canMarshalTo = Array (ContentTypes.`application/json`,
                               ContentType(MediaTypes.`text/xml`))

     Marshaller.of[Comments] (canMarshalTo:_*) { (value, requestedContentType, ctx) ⇒ {
       val content:String = value.to(requestedContentType.mediaType)
       ctx.marshalTo(HttpEntity(requestedContentType, content))
       }
     }
   }
  
  /** insert a comment  */
  def insertComment(genre: String, tuneId: String, comment: Comment): Validation[String, String] = 
    CommentsModel().insertComment(genre, tuneId, comment) 

  /** get a comment by key*/
  def getComment(genre: String, tuneId: String, user: String, cid: String) : Validation[String, Comment] =
    CommentsModel().getComment(genre, tuneId, user, cid)

  /** delete a comment by key */
  def deleteComment(genre: String, tuneId: String, user: String, cid: String) : Validation[String, String] =
    CommentsModel().deleteComment(genre, tuneId, user, cid)
    
   /** get all comments */
  def getComments(genre: String, tuneId: String ): Seq[Comment] =
    CommentsModel().getComments(genre, tuneId) 
  
  /* delete all comments */
  def deleteAllComments(genre: String) : Validation[String, String] = 
      CommentsModel().deleteAllComments(genre) 
    
}