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
