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
  }

  protected val numericCompOperators: Map[String, String] = Map(("_eq", "="), ("_neq", "!="), ("_gt", ">"), ("_lt", "<"), ("_gte", ">="), ("_lte", "<="), ("_in", "in"), ("_nin", "not in"), ("_is_null", "is null"))

  protected val stringCompOperators: Map[String, String] = numericCompOperators ++ Map(("_like", "like"), ("_nlike", "not like"))
}
