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

package it.pagopa.dbtographql.database

import java.sql.{Connection, ResultSet, Types}

import it.pagopa.dbtographql.database.DatabaseDataModel.QueryResult
import org.slf4j.LoggerFactory

import scala.annotation.switch

trait DatabaseDataMgmt {

  private val logger = LoggerFactory.getLogger(classOf[DatabaseDataMgmt])

  type Row = Array[Any]

  @inline def getRowField(resultSet: ResultSet, index: Int, columnType: Int): Any =
    (columnType: @switch) match {
      case Types.BOOLEAN => resultSet.getBoolean(index)
      case Types.DECIMAL => scala.math.BigDecimal(resultSet.getBigDecimal(index))
      case Types.VARCHAR => resultSet.getString(index)
      case Types.DOUBLE => resultSet.getDouble(index)
      case Types.REAL => resultSet.getDouble(index)
      case Types.FLOAT => resultSet.getFloat(index)
      case Types.SMALLINT => resultSet.getShort(index).toInt
      case Types.TINYINT => resultSet.getByte(index).toInt
      case Types.INTEGER => resultSet.getInt(index)
      case Types.BIGINT => resultSet.getLong(index)
      case Types.TIMESTAMP => resultSet.getTimestamp(index)
      case Types.DATE => resultSet.getDate(index)
      case 2003 => resultSet.getObject(index) //MAP
    }

  def getQueryResult(query: String, connection: Connection): QueryResult = {
    logger.info(s"getQueryResult => about to use connection ${connection.hashCode().toString}")
    val stmnt = connection.createStatement()
    val resultSet = stmnt.executeQuery(query)
    QueryResult(
      resultSet,
      LazyList.from(
        new Iterator[Unit] {
          override def hasNext: Boolean = resultSet.next()

          override def next(): Unit = ()
        }
      )
    )
  }

}
