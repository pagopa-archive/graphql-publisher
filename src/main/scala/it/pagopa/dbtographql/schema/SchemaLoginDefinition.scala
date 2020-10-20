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

package it.pagopa.dbtographql.schema

import it.pagopa.dbtographql.sessionmanagement.WithLogin
import sangria.schema.{Argument, Field, ObjectType, Schema, StringType, fields}

@SuppressWarnings(
  Array(
    "org.wartremover.warts.Any",
    "org.wartremover.warts.Nothing"
  )
)
trait SchemaLoginDefinition extends WithLogin {

  protected def generateLoginSchema: Schema[Ctx, Any] = {
    val UserNameArg = Argument("username", StringType)

    val PasswordArg = Argument("password", StringType)

    val DatabaseArg = Argument("database", StringType)

    val TokenArg = Argument("token", StringType)

    val MutationType = ObjectType("Mutation", fields[Ctx, Any](
      Field("login", StringType,
        arguments = UserNameArg :: PasswordArg :: DatabaseArg :: Nil,
        resolve = ctx =>
          login(
            ctx.args.arg[String]("username"),
            ctx.args.arg[String]("password"),
            ctx.args.arg[String]("database"),
          )
      ),
      Field("logout", StringType,
        arguments = TokenArg :: Nil,
        resolve = ctx =>
          logout(
            ctx.args.arg[String]("token")
          )
      ),
    ))

    Schema(ObjectType("Query", fields[Ctx, Any](
      Field("LoginLogout", StringType, resolve = _ => "")
    )), Some(MutationType))

  }

}
