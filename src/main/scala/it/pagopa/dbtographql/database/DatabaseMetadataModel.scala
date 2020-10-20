package it.pagopa.dbtographql.database

object DatabaseMetadataModel {

  final case class ColumnMetadata(columnName: String, columnTypeName: String, columnType: Int)

  final case class TableMetadata(tableName: String, databaseName: String, columns: List[ColumnMetadata])

  final case class DatabaseMetadata(databaseName: String, tables: List[TableMetadata])

}
