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

package it.pagopa.dbtographql

import java.time.temporal.TemporalAmount

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.MethodDirectives.get
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.http.scaladsl.server.{Route, StandardRoute}
import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport._
import io.circe._
import io.circe.optics.JsonPath._
import io.circe.parser._
import it.pagopa.dbtographql.common.ApplicationConfiguration
import it.pagopa.dbtographql.database.{DatabaseDataMgmt, DatabaseMetadataMgmt}
import it.pagopa.dbtographql.http.TemplatedFileAndResourceDirectives
import it.pagopa.dbtographql.schema.{Ctx, SchemaDefinition, SchemaLoginDefinition}
import it.pagopa.dbtographql.sessionmanagement.SessionManagement
import org.slf4j.LoggerFactory
import sangria.ast.Document
import sangria.execution.Executor
import sangria.marshalling.circe._
import sangria.parser.DeliveryScheme.Try
import sangria.parser.{QueryParser, SyntaxError}

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.{Duration, DurationInt}
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

@SuppressWarnings(
  Array(
    "org.wartremover.warts.Any",
    "org.wartremover.warts.Nothing",
    "org.wartremover.warts.Throw",
    "org.wartremover.warts.MutableDataStructures"
  )
)
object Main extends App with SessionManagement with DatabaseMetadataMgmt with DatabaseDataMgmt with SchemaDefinition with SchemaLoginDefinition with TemplatedFileAndResourceDirectives with CorsSupport {

  override def getConnectionUri: String = ApplicationConfiguration.jdbcUrl

  override def getPrivateKeyPath: String = ApplicationConfiguration.privateKeyPath

  override def getPublicKeyPath: String = ApplicationConfiguration.publicKeyPath

  override def getTokenLifetime: TemporalAmount = ApplicationConfiguration.tokenLifetime

  private val logger = LoggerFactory.getLogger(Main.getClass)

  logger.info("Starting Main ...")

  implicit private val system: ActorSystem = ActorSystem("server")

  import system.dispatcher

  def executeGraphQL(token: String, query: Document, variables: Json): StandardRoute = {
    val schema = getSchema(token)
    schema match {
      case Failure(exception) =>
        logger.error(s"Wrong schema:\n ${exception.getMessage}")
      case Success(_) =>
    }
    complete(
      Executor.execute(
        schema.fold(_ => generateLoginSchema, identity),
        query,
        Ctx(getConnectionFromToken(token).fold(t =>None, c => Some(c))),
        variables = variables
      )
    )
  }

  private def formatError(error: Throwable) = error match {
    case syntaxError: SyntaxError =>
      Json.obj("errors" -> Json.arr(
        Json.obj(
          "message" -> Json.fromString(syntaxError.getMessage),
          "locations" -> Json.arr(Json.obj(
            "line" -> Json.fromBigInt(syntaxError.originalError.position.line),
            "column" -> Json.fromBigInt(syntaxError.originalError.position.column))))))
    case NonFatal(e) =>
      formatMessage(e.getMessage)
    case e =>
      throw e
  }

  private def formatMessage(message: String) = Json.obj("errors" -> Json.arr(Json.obj("message" -> Json.fromString(message))))

  val route: Route =
    path("graphql") {
      headerValueByName("X-Token") { token =>
            post {
              withRequestTimeout(300.seconds) {
                entity(as[Json]) { body =>
                  val query = root.query.string.getOption(body)
                  val variablesStr = root.variables.string.getOption(body)
                  query.map(QueryParser.parse(_)) match {
                    case Some(Success(ast)) =>
                      variablesStr.map(parse) match {
                        case Some(Left(error)) => complete(BadRequest, formatError(error))
                        case Some(Right(json)) => executeGraphQL(token, ast, json)
                        case None => executeGraphQL(token, ast, root.variables.json.getOption(body) getOrElse Json.obj())
                      }
                    case Some(Failure(error)) => complete(BadRequest, formatError(error))
                    case None => complete(BadRequest, formatMessage("No query to execute"))
              }
            }
          }
        }
      }
    } ~
      path("graphql" / "ui") {
        optionalCookie("X-Token") { cookie => {
          get(
            cookie.fold(
              getFromResource("login.html")
            )(
              c =>
              deleteCookie("X-Token")(
                getFromResourceTemplated("graphql-playground.html", mutable.HashMap("token" -> c.value))
              )
            )
          )}
        }
      } ~
      path("graphql" / "ui") {
        post (
          formFields("username", "password", "database") { (username, password, database) =>
            val token = login(username, password, database)
            complete(token)
          }
        )
      } ~
      path("ping") {
        get {
          complete(HttpEntity.apply("pong"))
        }
      }

  private val bindingFuture = Http().newServerAt("0.0.0.0", 8088).bind(corsHandler(route))
  locally {
    val _ = Await.result(bindingFuture, Duration.Inf)
  }
}
