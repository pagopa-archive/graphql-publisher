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

import cats.implicits.catsSyntaxEq
import it.pagopa.dbtographql.database.DatabaseMetadataMgmt
import it.pagopa.dbtographql.database.DatabaseMetadataModel.TableMetadata
import sangria.schema.InputObjectType.DefaultInput
import sangria.schema.{BigDecimalType, BigIntType, BooleanType, FloatType, InputField, InputObjectType, IntType, ListInputType, LongType, OptionInputType, ScalarType, StringType}

import scala.reflect.runtime.universe.TypeTag

@SuppressWarnings(
  Array(
    "org.wartremover.warts.Any",
    "org.wartremover.warts.Nothing",
    "org.wartremover.warts.ToString",
    "org.wartremover.warts.Throw"
  )
)
trait SchemaDefinitionSupportWhereClause extends SchemaDefinitionSupportCommons with DatabaseMetadataMgmt {
  protected def getComparisonExp[T](tpe: ScalarType[T])(implicit tag: TypeTag[T]): InputObjectType[DefaultInput] = {

    val typeName = tag.tpe.erasure.typeSymbol.name.toString

    val isTime = typeName === "Timestamp" || typeName === "Date"
    val isNumeric = tag.tpe.erasure.typeSymbol.asClass.isNumeric || typeName === "BigInt" || typeName === "BigDecimal" || typeName === "Boolean" || typeName === "Timestamp" || typeName === "Date"

    val operators =
      if (isNumeric)
        numericCompOperators
      else if (isTime)
        numericCompOperators
      else if (typeName === "String")
        stringCompOperators
      else
        throw new Exception("Wrong type")

    InputObjectType(
      name = s"${typeName}_comparison_exp",
      description = s"expression to compare columns of type $typeName. All fields are combined with logical 'AND'",
      fields = operators.keySet.toList.map {
        case op@"_is_null" =>
          InputField(
            name = op,
            fieldType = OptionInputType(BooleanType)
          )
        case op@("_in" | "_nin") =>
          InputField(
            name = op,
            fieldType = OptionInputType(ListInputType(tpe))
          )
        case op =>
          InputField(
            name = op,
            fieldType = OptionInputType(tpe)
          )
      }
    )
  }

  protected def tableBooleanExp(table: TableMetadata): InputObjectType[DefaultInput] = {
    lazy val iot: InputObjectType[DefaultInput] = InputObjectType(
      s"${table.tableName}_bool_exp",
      s"""Boolean expression to filter rows from the table "${table.tableName}". All fields are combined with a logical 'AND'.""",
      () => List(
        InputField(
          name = "_and",
          fieldType = OptionInputType(ListInputType(OptionInputType(iot)))
        ),
        InputField(
          name = "_or",
          fieldType = OptionInputType(ListInputType(OptionInputType(iot)))
        ),
        InputField(
          name = "_not",
          fieldType = OptionInputType(iot)
        )
      ) ++ table.columns.map(c =>
        InputField(
          name = c.columnName,
          fieldType = OptionInputType(
            getFieldType(c.columnType).name match {
              case "Float" => getComparisonExp(FloatType)
              case "Double" => getComparisonExp(FloatType)
              case "String" => getComparisonExp(StringType)
              case "Int" => getComparisonExp(IntType)
              case "Long" => getComparisonExp(LongType)
              case "Boolean" => getComparisonExp(BooleanType)
              case "BigInt" => getComparisonExp(BigIntType)
              case "BigDecimal" => getComparisonExp(BigDecimalType)
              case "Timestamp" => getComparisonExp(TimestampType)
              case "Date" => getComparisonExp(DateType)
            }
          )
        )
      )
    )
    iot
  }
}
