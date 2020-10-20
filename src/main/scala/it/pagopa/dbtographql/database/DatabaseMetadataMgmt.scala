package it.pagopa.dbtographql.database

import java.sql.{Connection, ResultSet, Types}

import cats.implicits.catsSyntaxEq
import it.pagopa.dbtographql.database.DatabaseMetadataModel.{ColumnMetadata, DatabaseMetadata, TableMetadata}
import org.slf4j.LoggerFactory

import scala.annotation.{switch, tailrec}

@SuppressWarnings(
  Array(
    "org.wartremover.warts.Null"
  )
)
trait DatabaseMetadataMgmt {

  private val logger = LoggerFactory.getLogger(classOf[DatabaseMetadataMgmt])

  @inline protected def isNumericField(columnType: Int): Boolean = {
    (columnType: @switch) match {
      case Types.DOUBLE | Types.FLOAT | Types.SMALLINT | Types.INTEGER | Types.BIGINT | Types.DECIMAL => true
      case _ => false
    }
  }

  protected def resultSetToList(resultSet: ResultSet): List[Seq[(String, AnyRef, String)]] = {
    val metaData = resultSet.getMetaData
    val columnCount = resultSet.getMetaData.getColumnCount

    @inline def getRow: Seq[(String, AnyRef, String)] = (1 to columnCount).
      map(
        i => (
          metaData.getColumnName(i),
          resultSet.getObject(i),
          metaData.getColumnTypeName(i)
        )
      )

    @tailrec
    def resultSetToList(resultSet: ResultSet, list: List[Seq[(String, AnyRef, String)]]): List[Seq[(String, AnyRef, String)]] = {
      if (resultSet.next())
        resultSetToList(resultSet, getRow :: list)
      else
        list
    }

    resultSetToList(resultSet, List.empty[Seq[(String, AnyRef, String)]])
  }

  protected def getDatabaseMetadata(connection: Connection, database: String): DatabaseMetadataModel.DatabaseMetadata = {
    logger.info(s"getDbInfo => about to use connection ${connection.hashCode().toString}")
    val metaData = connection.getMetaData
    val resultSet = metaData.getTables(null, null, null, Array("TABLE"))
    val tables = resultSetToList(resultSet).filter(r => r(1)._2.asInstanceOf[String] === database).map(r => r(2)._2.asInstanceOf[String])
    DatabaseMetadata(
      database,
      tables.map {
        table =>
          val columns = resultSetToList(metaData.getColumns(null, null, table, null)).
            map(r => ColumnMetadata(r(3)._2.asInstanceOf[String], r(5)._2.asInstanceOf[String], r(4)._2.asInstanceOf[Int])).
            distinct
          TableMetadata(table, database, columns)
      }
    )
  }

}
