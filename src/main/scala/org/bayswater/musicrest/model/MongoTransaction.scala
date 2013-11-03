package org.bayswater.musicrest.model


import scalaz.Validation
import scalaz.syntax.validation._
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.MongoDB

trait MongoTransaction { 
  
    /** Apparently, Mongo doesn't do transactions - this is the nearest we get - it ties the sequence of statements
   * to one particular Mongo connection and (we hope) raises an exception against that connection if things go wrong.
   */
  def withMongoPseudoTransction [A] (mongoDB: MongoDB) (body:  =>  A) : Validation[String, A] = 
    try {
      mongoDB.requestStart
      val b = body
      mongoDB.getLastError.throwOnError
      b.success
    }
    catch {
       case e: Exception  => e.getMessage().fail      
    }
    finally {
      mongoDB.requestDone      
    }

}