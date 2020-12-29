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

import java.sql.{Date, Timestamp, Types}

import sangria.ast.StringValue
import sangria.schema.{BigDecimalType, BooleanType, FloatType, IntType, LongType, ScalarType, StringType}
import sangria.validation.Violation

import scala.util.Try

@SuppressWarnings(
  Array(
    "org.wartremover.warts.Any",
    "org.wartremover.warts.Nothing"
  )
)
trait SchemaDefinitionSupportCommons {

  case object DateCoerceViolation extends Violation {
    override def errorMessage: String = "Error during parsing Date"
  }

  implicit val DateType: ScalarType[Date] = ScalarType[Date](
    "Date",
    coerceOutput = (ts, _) =>
      ts.toString,
    coerceInput = {
      case StringValue(dt, _, _, _, _) =>
        Try(Date.valueOf(dt)).toOption.toRight(TimestampCoerceViolation)
      case _ =>
        Left(TimestampCoerceViolation)
    },
    coerceUserInput = {
      case s: String => Try(Date.valueOf(s)).toOption.toRight(DateCoerceViolation)
      case _ => Left(DateCoerceViolation)
    }
  )

  case object TimestampCoerceViolation extends Violation {
    override def errorMessage: String = "Error during parsing Timestamp"
  }

  implicit val TimestampType: ScalarType[Timestamp] = ScalarType[Timestamp](
    "Timestamp",
    coerceOutput = (ts, _) =>
      ts.toString,
    coerceInput = {
      case StringValue(ts, _, _, _, _) =>
        val parsedTs = Try(Timestamp.valueOf(ts))
        parsedTs.toOption.toRight(TimestampCoerceViolation)
      case _ =>
        Left(TimestampCoerceViolation)
    },
    coerceUserInput = {
      case s: String => Try(Timestamp.valueOf(s)).toOption.toRight(TimestampCoerceViolation)
      case _ => Left(TimestampCoerceViolation)
    }
  )


  @inline protected def getFieldType(columnType: Int): ScalarType[_ >: Double with String with Int with Boolean with Long with BigDecimal with Date with Timestamp] = columnType match {
    case Types.FLOAT => FloatType
    case Types.DOUBLE => FloatType
    case Types.REAL => FloatType
    case Types.CHAR => StringType
    case Types.VARCHAR => StringType
    case Types.LONGVARCHAR => StringType
    case Types.TINYINT => IntType
    case Types.SMALLINT => IntType
    case Types.INTEGER => IntType
    case Types.BOOLEAN => BooleanType
    case Types.BIGINT => LongType
    case Types.DECIMAL => BigDecimalType
    case Types.NUMERIC => BigDecimalType
    case Types.DATE => DateType
    case Types.TIMESTAMP => TimestampType
    case Types.BIT => BooleanType
    case 2003 => StringType
  }

  protected val numericCompOperators: Map[String, String] = Map(("_eq", "="), ("_neq", "!="), ("_gt", ">"), ("_lt", "<"), ("_gte", ">="), ("_lte", "<="), ("_in", "in"), ("_nin", "not in"), ("_is_null", "is null"))

  protected val stringCompOperators: Map[String, String] = numericCompOperators ++ Map(("_like", "like"), ("_nlike", "not like"))
}
