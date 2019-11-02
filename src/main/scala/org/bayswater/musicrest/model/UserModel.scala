package org.bayswater.musicrest.model

import org.bayswater.musicrest.MusicRestSettings
import com.mongodb.casbah.Imports._
import com.mongodb.ServerAddress
import scalaz.Validation

trait UserModel {

  /** see if the user exists */
  def existsUser(id: String) : Boolean

  /** insert a (not yet validated) user into the database */
  def insertUser(user: User): Validation[String, UnregisteredUser]

  /** insert a user and set his registration status */
  def insertUser(user: User, isRegistered: Boolean): Validation[String, UnregisteredUser]

  /** insert a new user who'se automatically registered */
  def insertPreRegisteredUser(user: User): Validation[String, UnregisteredUser]

  /** get the user details */
  def getUser(name: String) : Validation[String, UserRef]

  /** set the user's validity */
  def validateUser(uuid: String): Validation[String, UnregisteredUser]

  /** get a list of users */
  def getUsers(page: Int, size: Int): Iterator[UserRef]

  /** check the incoming credentials to see if this is a valid user */
  def isValidUser(name: String, password:String): Boolean

  /** count users */
  def userCount() : Long

  /** alter a user's password */
  def alterPassword(name: String, password: String) : Validation[String, String]

  /** delete user */
  def deleteUser(id: String) : Validation[String, String]

  /** delete all users */
  def deleteUsers : Validation[String, String]

}

object UserModel {
  private val settings = MusicRestSettings

  private val mongoClient =
     MongoCasbahUtil.buildMongoClient(  settings.dbHost
                                      , settings.dbPort
                                      , settings.dbLogin
                                      , settings.dbPassword
                                      , settings.dbName
                                      , Some(settings.dbPoolSize)
                                      )


  private val casbahUserModel = new UserModelCasbahImpl(mongoClient, settings.dbName)
  def apply(): UserModel = casbahUserModel
}
