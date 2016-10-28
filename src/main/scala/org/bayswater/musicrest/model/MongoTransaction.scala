package org.bayswater.musicrest.model


import scalaz.Validation
import scalaz.syntax.validation._
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.MongoDB

trait MongoTransaction { 
  
  /** Apparently, Mongo doesn't do transactions - this is the nearest we get - it ties the sequence of statements
   * to one particular Mongo connection and (we hope) raises an exception against that connection if things go wrong.
   *
   * We've now lost requestStart, getLastError.throwOnError, requestDone  for transaction management
   * but now we catch exceptions straightforwardly, I guess it's no longer needed
   */
  def withMongoPseudoTransction [A] (mongoDB: MongoDB) (body:  =>  A) : Validation[String, A] = 
    try {
      // mongoDB.requestStart
      val b = body
      // mongoDB.getLastError.throwOnError
      b.success
    }
    catch {
       case me: com.mongodb.DuplicateKeyException  => {
         println("MONGO Duplicate key EXCEPTION ")
         me.getMessage().failure[A]      
       }
       case e: Exception  => {
         println(s"MONGO EXCEPTION type: ${e.getClass()}")
         e.getMessage().failure[A]     
       }
    }
    finally {
      // mongoDB.requestDone      
    }


}
