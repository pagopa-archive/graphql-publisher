package it.pagopa.dbtographql.schema

import java.sql.{Connection, ResultSet}

@SuppressWarnings(
  Array(
    "org.wartremover.warts.DefaultArguments",
    "org.wartremover.warts.Var"
  )
)
final case class Ctx(connection: Option[Connection] = None, var currentResultSet: Option[(ResultSet, Map[String, (Int, Int)])] = None)
