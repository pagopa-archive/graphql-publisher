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

import java.sql.Types

import cats.implicits.catsSyntaxEq
import it.pagopa.dbtographql.database.DatabaseMetadataMgmt
import it.pagopa.dbtographql.database.DatabaseMetadataModel.TableMetadata
import org.slf4j.LoggerFactory
import sangria.ast._
import sangria.schema.InputObjectType.DefaultInput
import sangria.schema.{Context, InputObjectType, ObjectType}

import scala.collection.immutable.ListMap

@SuppressWarnings(
  Array(
    "org.wartremover.warts.Any",
    "org.wartremover.warts.Nothing",
    "org.wartremover.warts.OptionPartial",
    "org.wartremover.warts.AsInstanceOf",
    "org.wartremover.warts.Recursion",
    "org.wartremover.warts.Throw",
  )
)
trait SchemaDefinitionSupportSqlGen extends SchemaDefinitionSupportCommons with DatabaseMetadataMgmt {

  private val logger = LoggerFactory.getLogger(classOf[SchemaDefinitionSupportSqlGen])

  case class SchemaTable(tableMetadata: TableMetadata, table: ObjectType[Ctx, Any], aggregatedTable: ObjectType[Ctx, Any], orderBy: InputObjectType[DefaultInput])

  protected def generateSelectQuery(schemaTable: SchemaTable, ctx: Context[Ctx, Any], limit: Option[Int], allColumns: Boolean): String = {
    val whereArg = for {
      field <- ctx.astFields.toList.headOption
      whereArg <- field.arguments.map(a => (a.name, a)).toMap.get("where")
    } yield
      whereArg
    val whereClause = whereArg.map(generateWhereClause(_, schemaTable.tableMetadata.columns.map(c => (c.columnName, c.columnType)).toMap))

    val orderBy = ctx.argOpt[ListMap[String, Option[String]]]("order_by")
    val orderByClause = s"${orderBy.fold("")(_.map(p => s"${p._1} ${p._2.get}").mkString("order by ", ",", ""))}"
    val distinctOn = ctx.argOpt[String]("distinct_on")
    val selectedFields = ctx.astFields(0).selections.map(_.renderCompact)
    val selectedColumns = if (allColumns)
      schemaTable.tableMetadata.columns
    else
      schemaTable.tableMetadata.columns.map(ci => (ci.columnName, ci)).toMap.filter(p => selectedFields.contains(p._1)).values.toList
    distinctOn.fold(
      s"""
         |select ${selectedColumns.map(s => s"`${s.columnName}`").mkString(",")} from ${schemaTable.tableMetadata.databaseName}.${schemaTable.tableMetadata.tableName}
         |${whereClause.fold("")(w => s"where $w")}
         |$orderByClause
         |${limit.fold("")(l => s"limit ${l.toString}")}
         |""".stripMargin
    )(distinctOn => {
      s"""
         |with summary AS (
         |    select *,
         |           row_number() over(partition by $distinctOn
         |                             order     by $distinctOn desc) as rk
         |    from ${schemaTable.tableMetadata.databaseName}.${schemaTable.tableMetadata.tableName}
         |)
         |select ${selectedColumns.map(s => s"`${s.columnName}`").mkString(",")} from summary
         |where rk = 1 $orderByClause ${limit.fold("")(l => s"limit ${l.toString}")}
         |
         |""".stripMargin
    })
  }

  protected def generateAggregatedFields(ctx: Context[Ctx, Any]): String = {
    val aggregatedFields = ctx.astFields(0).selections.asInstanceOf[Seq[sangria.ast.Field]]
    val sqlAvgAggregatedFields = aggregatedFields.filter(_.name === "avg").flatMap(_.selections).map(_.asInstanceOf[sangria.ast.Field].name).toSet.map((f: String) => s"avg($f)").mkString(",")
    val sqlMaxAggregatedFields = aggregatedFields.filter(_.name === "max").flatMap(_.selections).map(_.asInstanceOf[sangria.ast.Field].name).toSet.map((f: String) => s"max($f)").mkString(",")
    val sqlMinAggregatedFields = aggregatedFields.filter(_.name === "min").flatMap(_.selections).map(_.asInstanceOf[sangria.ast.Field].name).toSet.map((f: String) => s"min($f)").mkString(",")
    val sqlCountAggregatedFields = aggregatedFields.filter(_.name === "count").toList.headOption.fold("")(f => s"count(${
      f.arguments.map(a => (a.name, a.value)).toMap.get("distinct").fold("")(v =>
        if (v.asInstanceOf[BooleanValue].value)
          "distinct "
        else "")
    }${
      f.arguments(0).value.renderPretty
    })")
    val sqlAggregatedFields = List(sqlAvgAggregatedFields, sqlMaxAggregatedFields, sqlMinAggregatedFields, sqlCountAggregatedFields).filter(!_.isEmpty)
    s"""
       | ${sqlAggregatedFields.mkString(",")}
       |""".stripMargin
  }

  protected def generateAggregatedQuery(schemaTable: SchemaTable, ctx: Context[Ctx, Any]): String = {
    val sqlQuery = generateSelectQuery(schemaTable, ctx, None, allColumns = true)
    val aggregatedFields = generateAggregatedFields(ctx)
    s"""
       |with selected_table as (
       | $sqlQuery
       |)
       |select $aggregatedFields from selected_table
       |""".stripMargin
  }

  protected def generateWhereClause(whereClauseArgument: Argument, columns: Map[String, Int]): String = {
    val whereNode = whereClauseArgument.value

    @inline def generateField(name: String, fields: Vector[ObjectField], columnType: Int): String = {
      fields.map(field => {
        val sqlOp = field.name match {
          case "_is_null" if field.value.asInstanceOf[BooleanValue].value =>
            "is null"
          case "_is_null" if !field.value.asInstanceOf[BooleanValue].value =>
            "is not null"
          case "_in" =>
            val numbers = field.value.asInstanceOf[ListValue].values.map(_.renderPretty).mkString("(", ",", ")")
            s"${stringCompOperators(field.name)} $numbers"
          case "_nin" =>
            s"${stringCompOperators(field.name)} (1, 100, 1000)"
          case _ if columnType === Types.TIMESTAMP | columnType === Types.DATE =>
            s"${stringCompOperators(field.name)} ${field.value.renderPretty.replace("\"", "'")}"
          case _ =>
            s"${stringCompOperators(field.name)} ${field.value.renderPretty}"
        }
        s"$name $sqlOp"
      }).mkString(" AND ")
    }

    def generate(astNode: AstNode): String = {
      astNode match {
        case ObjectValue(Vector(ObjectField("_or", ObjectValue(fields, _, _), _, _)), _, _) =>
          fields.map(generate(_)).mkString("(", " OR ", ")")
        case ObjectValue(Vector(ObjectField("_and", ObjectValue(fields, _, _), _, _)), _, _) =>
          fields.map(generate(_)).mkString("(", " AND ", ")")
        case ObjectField(name, ObjectValue(fields, _, _), _, _) if columns.contains(name) =>
          generateField(name, fields, columns(name))
        case ObjectValue(Vector(ObjectField(name, ObjectValue(fields, _, _), _, _)), _, _) if columns.contains(name) =>
          generateField(name, fields, columns(name))
        case ObjectField("_or", ObjectValue(fields, _, _), _, _) =>
          fields.map(generate(_)).mkString("(", " OR ", ")")
        case ObjectField("_and", ObjectValue(fields, _, _), _, _) =>
          fields.map(generate(_)).mkString("(", " AND ", ")")
        case _ =>
          throw new Exception("It shouldn't be here")
      }
    }

    val whereClause = generate(whereNode)
    logger.info(s"generateWhereClause: $whereClause")
    whereClause
  }
}
