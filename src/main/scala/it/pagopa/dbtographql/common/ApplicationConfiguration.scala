package it.pagopa.dbtographql.common

import java.time.temporal.TemporalAmount

import com.typesafe.config.ConfigFactory

import scala.util.Try

object ApplicationConfiguration {

  private val config = ConfigFactory.load()

  val privateKeyPath: String = Try(config.getString("graphql-publisher.private-key-path")).getOrElse("etc/key.priv")

  val publicKeyPath: String = Try(config.getString("graphql-publisher.public-key-path")).getOrElse("etc/key.pub")

  val jdbcUrl: String = config.getString("graphql-publisher.jdbc-url")

  val rowsLimit: Int = config.getInt("graphql-publisher.rows-limit")

  val tokenLifetime: TemporalAmount = config.getTemporal("graphql-publisher.token-lifetime")

}
