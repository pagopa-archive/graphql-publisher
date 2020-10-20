package it.pagopa.dbtographql.database

import java.sql.ResultSet

object DatabaseDataModel {

  final case class QueryResult(resultSet: ResultSet, rowIndex: LazyList[Unit])

}
