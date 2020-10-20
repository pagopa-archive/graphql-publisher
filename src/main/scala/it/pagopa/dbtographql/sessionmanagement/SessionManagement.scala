package it.pagopa.dbtographql.sessionmanagement

import java.security.interfaces.{RSAPrivateKey, RSAPublicKey}
import java.sql.{Connection, DriverManager}
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
import it.pagopa.dbtographql.sessionmanagement.SessionManagement._
import org.slf4j.LoggerFactory
import sangria.schema.Schema

@SuppressWarnings(
  Array(
    "org.wartremover.warts.Any",
    "org.wartremover.warts.Nothing",
    "org.wartremover.warts.AsInstanceOf",
    "org.wartremover.warts.Throw"
  )
)
trait SessionManagement {

  private val logger = LoggerFactory.getLogger(classOf[SessionManagement])

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

  protected def getConnection(uri: String, username: String, password: String): Connection = {
    val props = new Properties()
    val _ = props.setProperty("user", username)
    val _ = props.setProperty("password", password)
    DriverManager.getConnection(uri, props)
  }

  protected def getSchema(token: String): Schema[Ctx, Any] = {
    if (token === "login" || !sessions.isDefinedAt(token))
      generateLoginSchema
    else {
      val jwt = EncryptedJWT.parse(token)
      jwt.decrypt(decrypter)
      val username = jwt.getJWTClaimsSet.getClaim("username").asInstanceOf[String]
      val password = jwt.getJWTClaimsSet.getClaim("password").asInstanceOf[String]
      val database = jwt.getJWTClaimsSet.getClaim("database").asInstanceOf[String]
      val expirationTime = jwt.getJWTClaimsSet.getExpirationTime
      val now = new Date()
      if (now.after(expirationTime)) {
        sessions(token).connection.close()
        sessions = sessions - token
        logger.info(s"Token expired")
        generateLoginSchema
      } else {
        val sessionEntry = sessions(token)
        if (sessionEntry.connection.isClosed || !sessionEntry.connection.isValid(1000)) { //The connection is no more valid
          sessions = sessions - token
          val connection = getConnection(getConnectionUri, username, password)
          val databaseMetadata = getDatabaseMetadata(connection, database)
          val schema = generateSchema(databaseMetadata)
          sessions = sessions + (token -> SessionEntry(schema, connection))
        }
        sessions(token).schema
      }
    }
  }

  protected def getSessionConnection(token: String): Option[Connection] = sessions.get(token).map(_.connection)

  protected def getSessionUsername(token: String): String = {
    val jwt = EncryptedJWT.parse(token)
    jwt.decrypt(decrypter)
    jwt.getJWTClaimsSet.getClaim("username").asInstanceOf[String]
  }

}

@SuppressWarnings(
  Array(
    "org.wartremover.warts.Any",
    "org.wartremover.warts.Var"
  )
)
object SessionManagement {

  final case class SessionEntry(schema: Schema[Ctx, Any], connection: Connection)

  var sessions: Map[String, SessionEntry] = Map.empty[String, SessionEntry]
}
