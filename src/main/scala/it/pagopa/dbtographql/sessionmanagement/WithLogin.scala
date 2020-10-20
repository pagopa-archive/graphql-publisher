package it.pagopa.dbtographql.sessionmanagement

import java.sql.Connection

import cats.implicits.catsSyntaxEq
import it.pagopa.dbtographql.database.DatabaseMetadataModel.DatabaseMetadata
import it.pagopa.dbtographql.schema.Ctx
import it.pagopa.dbtographql.sessionmanagement.SessionManagement.{sessions, _}
import org.slf4j.LoggerFactory
import sangria.schema.Schema

@SuppressWarnings(
  Array(
    "org.wartremover.warts.Any"
  )
)
trait WithLogin {

  private val logger = LoggerFactory.getLogger(classOf[WithLogin])

  def getConnectionUri: String

  protected def getConnection(uri: String, username: String, password: String): Connection

  protected def generateToken(username: String, password: String, database: String): String

  protected def getSessionUsername(token: String): String

  protected def getDatabaseMetadata(connection: Connection, database: String): DatabaseMetadata

  protected def generateSchema(metadata: DatabaseMetadata): Schema[Ctx, Any]

  protected def login(username: String, password: String, database: String): String = {
    val connection = getConnection(getConnectionUri, username, password)
    val oldToken = sessions.keys.find(token => getSessionUsername(token) === username)
    oldToken.fold({
      val token = generateToken(username, password, database)
      val databaseMetadata = getDatabaseMetadata(connection, database)
      val schema = generateSchema(databaseMetadata)
      sessions = sessions + (token -> SessionEntry(schema, connection))
      logger.info(s"User $username logged in. Total number of sessions = ${sessions.size.toString}")
      token
    })(oldToken => {
      connection.close() //I reuse the old one
      logger.info(s"User $username already logged in. Closing the new connection ${connection.hashCode().toString}, returning the old token and keeping the old connection ${sessions(oldToken).connection.hashCode.toString}. Total number of sessions = ${sessions.size.toString}")
      oldToken
    })
  }

  protected def logout(token: String): String = {
    val session = sessions.get(token)
    session.fold(s"User ${getSessionUsername(token)} already logged out")(s => {
      s.connection.close()
      sessions = sessions - token
      s"User ${getSessionUsername(token)} logged out"
    })
  }

}
