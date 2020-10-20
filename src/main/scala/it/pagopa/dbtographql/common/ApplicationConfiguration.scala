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
