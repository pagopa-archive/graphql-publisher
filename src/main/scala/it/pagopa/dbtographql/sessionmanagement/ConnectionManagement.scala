/*
 * Copyright 2020 Pagopa S.p.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.pagopa.dbtographql.sessionmanagement

import java.security.interfaces.{RSAPrivateKey, RSAPublicKey}
import java.sql.{Connection, DriverManager, SQLException}
import java.time.temporal.{ChronoUnit, TemporalAmount}
import java.util.{Date, Properties}

import cats.implicits.{catsKernelStdOrderForString, catsSyntaxEq}
import com.auth0.jwt.algorithms.Algorithm
import com.nimbusds.jose.crypto.{RSADecrypter, RSAEncrypter}
import com.nimbusds.jose.{EncryptionMethod, JWEAlgorithm, JWEHeader}
import com.nimbusds.jwt.{EncryptedJWT, JWTClaimsSet}
import it.pagopa.dbtographql.common.PemUtils
import it.pagopa.dbtographql.database.DatabaseMetadataModel.DatabaseMetadata
import it.pagopa.dbtographql.schema.Ctx
import it.pagopa.dbtographql.sessionmanagement.ConnectionManagement.connections
import org.apache.thrift.transport.TTransportException
import org.slf4j.LoggerFactory
import sangria.schema.Schema

import scala.util.{Failure, Success, Try}

@SuppressWarnings(
  Array(
    "org.wartremover.warts.Any",
    "org.wartremover.warts.Nothing",
    "org.wartremover.warts.AsInstanceOf",
    "org.wartremover.warts.Throw"
  )
)
trait ConnectionManagement {

  private val logger = LoggerFactory.getLogger(classOf[ConnectionManagement])

  def getConnectionUri: String

  def getPrivateKeyPath: String

  def getPublicKeyPath: String

  def getTokenLifetime: TemporalAmount

  protected def getDatabaseMetadata(connection: Connection, database: String): DatabaseMetadata

  protected def generateSchema(metadata: DatabaseMetadata): Schema[Ctx, Any]

  protected def generateLoginSchema: Schema[Ctx, Any]

  protected val algorithm: Algorithm = {
    val privateKey = PemUtils.readPrivateKeyFromFile(getPrivateKeyPath, "RSA").asInstanceOf[RSAPrivateKey]
    val publicKey = PemUtils.readPublicKeyFromFile(getPublicKeyPath, "RSA").asInstanceOf[RSAPublicKey]
    Algorithm.RSA256(publicKey, privateKey)
  }

  protected val encrypter: RSAEncrypter = {
    val publicKey: RSAPublicKey = PemUtils.readPublicKeyFromFile(getPublicKeyPath, "RSA").asInstanceOf[RSAPublicKey]
    new RSAEncrypter(publicKey)
  }

  protected val decrypter: RSADecrypter = {
    val privateKey = PemUtils.readPrivateKeyFromFile(getPrivateKeyPath, "RSA").asInstanceOf[RSAPrivateKey]
    new RSADecrypter(privateKey)
  }

  protected def generateToken(username: String, password: String, database: String): String = {
    val expirationTime = new Date(new Date().getTime + getTokenLifetime.get(ChronoUnit.SECONDS)*1000L)
    val jwtClaims = new JWTClaimsSet.Builder().
      issuer("graphql-publisher").
      expirationTime(expirationTime).
      claim("username", username).
      claim("password", password).
      claim("database", database).
      build()
    val header = new JWEHeader(JWEAlgorithm.RSA_OAEP_256, EncryptionMethod.A128GCM)
    val jwt = new EncryptedJWT(header, jwtClaims)
    jwt.encrypt(encrypter)
    jwt.serialize
  }

  protected def decryptInfoFromToken(token: String): (String, String, String, Date) = {
    if(token === "login")
      ("login", "", "", new Date())
    else {
      val jwt = EncryptedJWT.parse(token)
      jwt.decrypt(decrypter)
      val username = jwt.getJWTClaimsSet.getClaim("username").asInstanceOf[String]
      val password = jwt.getJWTClaimsSet.getClaim("password").asInstanceOf[String]
      val database = jwt.getJWTClaimsSet.getClaim("database").asInstanceOf[String]
      val expirationTime = jwt.getJWTClaimsSet.getExpirationTime
      (username, password, database, expirationTime)
    }
  }

  protected def getConnection(uri: String, username: String, password: String): Try[Connection] = Try {
    val props = new Properties()
    val _ = props.setProperty("user", username)
    val _ = props.setProperty("password", password)
    try {
      DriverManager.getConnection(uri, props)
    } catch {
      case e: SQLException =>
        logger.error(s"Invalid username $username or password")
        throw new Exception(e)
      case e: TTransportException =>
        logger.error(s"Problem in connecting to the database")
        throw new Exception(e)
    }
  }

  protected def getConnectionFromToken(token: String): Try[Connection] = {
    val (username, password, _, expirationTime) = decryptInfoFromToken(token)
    if(username === "login") {
      logger.info(s"User not yet logged")
      Failure(new Exception("User not logged in"))
    } else {
      val now = new Date()
      if (now.after(expirationTime)) {
        logger.info(s"Token expired")
        connections.get(token).foreach(_.close())
        Failure(new Exception("Token Expired"))
      } else {
        connections.get(token).fold({
          val connection = getConnection(getConnectionUri, username, password)
          connection.foreach(c => connections = connections + (token -> c))
          connection
        })(c => {
          logger.info(s"Reusing connection ${c.hashCode().toString}")
          Success(c)
        })
      }
    }
  }

  protected def getSchema(token: String): Try[Schema[Ctx, Any]] = {
    if (token === "login")
      Success(generateLoginSchema)
    else {
      getConnectionFromToken(token).map {
        connection =>
          val jwt = EncryptedJWT.parse(token)
          jwt.decrypt(decrypter)
          val database = jwt.getJWTClaimsSet.getClaim("database").asInstanceOf[String]
          val databaseMetadata = getDatabaseMetadata(connection, database)
          generateSchema(databaseMetadata)
      }
    }
  }

  protected def getSessionUsername(token: String): String = {
    val jwt = EncryptedJWT.parse(token)
    jwt.decrypt(decrypter)
    jwt.getJWTClaimsSet.getClaim("username").asInstanceOf[String]
  }

}

@SuppressWarnings(
  Array(
    "org.wartremover.warts.Var"
  )
)
object ConnectionManagement {
  var connections: Map[String, Connection] = Map.empty[String, Connection]
}