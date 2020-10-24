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

import java.sql.Connection
import java.util.Date
import ConnectionManagement._
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

@SuppressWarnings(
  Array(
    "org.wartremover.warts.Any"
  )
)
trait WithLogin {

  private val logger = LoggerFactory.getLogger(classOf[WithLogin])

  def getConnectionUri: String

  protected def getConnection(uri: String, username: String, password: String): Try[Connection]

  protected def generateToken(username: String, password: String, database: String): String

  protected def decryptInfoFromToken(token: String): (String, String, String, Date)

  protected def login(username: String, password: String, database: String): String = {
    logger.info(s"About to login the user $username")
    val connection = getConnection(getConnectionUri, username, password)
    connection match {
      case Success(connection) =>
        logger.info(s"User $username logged in")
        val token = generateToken(username, password, database)
        connections = connections + (token -> connection)
        token
      case Failure(exception) =>
        logger.error(s"User $username login failed: ${exception.getMessage}")
        "login"
    }
  }

  protected def logout(token: String): String = {
    val (username, _, _, _) = decryptInfoFromToken(token)
    connections.get(token).fold(())(c => {
      c.close()
      connections = connections - token
    })
    s"User $username logged out"
  }

}
