package it.pagopa.dbtographql.schema

import java.sql.{Connection, ResultSet}

import it.pagopa.dbtographql.common.ApplicationConfiguration
import it.pagopa.dbtographql.database.DatabaseDataModel.QueryResult
import it.pagopa.dbtographql.database.DatabaseMetadataModel.DatabaseMetadata
import org.slf4j.LoggerFactory
import sangria.schema.{Argument, Context, EnumType, EnumValue, Field, InputField, InputObjectType, IntType, ListType, ObjectType, OptionInputType, Schema, StringType, fields}

@SuppressWarnings(
  Array(
    "org.wartremover.warts.Any",
    "org.wartremover.warts.Nothing",
    "org.wartremover.warts.Throw"
  )
)
trait SchemaDefinition extends SchemaDefinitionSupport with SchemaDefinitionSupportWhereClause with SchemaDefinitionSupportSqlGen {

  private val logger = LoggerFactory.getLogger(classOf[SchemaDefinition])

  protected def getRowField(resultSet: ResultSet, index: Int, columnType: Int): Any

  protected def getQueryResult(query: String, connection: Connection): QueryResult

  private def getSchemaTables(databaseMetadata: DatabaseMetadata): Seq[SchemaTable] = {
    val orderByType = EnumType("order_by", Some("column ordering options"), List(EnumValue("asc", Some("in the ascending order"), "asc"), EnumValue("desc", Some("in the descending order"), "desc")))
    databaseMetadata.tables.map(table => {
      SchemaTable(
        table,
        ObjectType(
          table.tableName,
          fields(
            table.columns.map(c => {
              Field[Ctx, Any, Any, Any](
                c.columnName,
                getFieldType(c.columnType),
                resolve = (ctx: Context[Ctx, Any]) => {
                  val name = ctx.field.name
                  (for {
                    crs <- ctx.ctx.currentResultSet
                  } yield (crs._1, crs._2)) match {
                    case Some((resultSet: ResultSet, header: Map[String, (Int, Int)])) =>
                      getRowField(resultSet, header(name)._2, header(name)._1)
                    case None =>
                      throw new Exception("Something went wrong")
                  }
                })
            }): _*)),
        ObjectType(
          s"${table.tableName}_aggregate",
          fields(
            List(
              getAggregateFieldType(table, Avg),
              getAggregateFieldType(table, Max),
              getAggregateFieldType(table, Min),
              getAggregateFieldType(table, Count)
            ).filter(_.isDefined).map(_.getOrElse(throw new Exception("Something went wrong"))): _*
          )),
        InputObjectType(
          s"${table.tableName}_order_by",
          table.columns.map(c =>
            InputField(
              c.columnName,
              OptionInputType(orderByType),
              description = ""
            )
          )
        )
      )
    })
  }

  def generateSchema(databaseMetadata: DatabaseMetadata): Schema[Ctx, Any] =
    if (databaseMetadata.tables.isEmpty)
      Schema[Ctx, Any](ObjectType("Query", fields[Ctx, Any](
        Field("Error", StringType, resolve = _ => "No table available")
      ))) else {
      val schemaTables: Seq[SchemaTable] = getSchemaTables(databaseMetadata)
      val query: ObjectType[Ctx, Any] = ObjectType(
        "Query",
        fields(
          schemaTables.filter(st => st.tableMetadata.columns.nonEmpty).flatMap(schemaTable => {
            val argOrderBy = Argument("order_by", OptionInputType(schemaTable.orderBy), "")
            val argLimit = Argument("limit", OptionInputType(IntType), "")
            val argWhere = Argument("where", OptionInputType(tableBooleanExp(schemaTable.tableMetadata)), "")
            val argDistinctOn = Argument("distinct_on", OptionInputType(tableSelectColumn(schemaTable.tableMetadata)), "")
            val selectArguments = argOrderBy :: argLimit :: argWhere :: argDistinctOn :: Nil
            List(
              Field[Ctx, Any, Any, Any](
                name = schemaTable.tableMetadata.tableName,
                fieldType = ListType(schemaTable.table),
                description = Some(s"""fetch data from the table: "${schemaTable.tableMetadata.tableName}""""),
                arguments = selectArguments,
                resolve = (ctx: Context[Ctx, Any]) => {
                  val limit = Integer.min(ApplicationConfiguration.rowsLimit, ctx.argOpt[Int]("limit").getOrElse(ApplicationConfiguration.rowsLimit))
                  val sqlQuery = generateSelectQuery(schemaTable, ctx, Some(limit), allColumns = false)
                  logger.info(sqlQuery)
                  val queryResult = (for {
                    con <- ctx.ctx.connection
                  } yield getQueryResult(sqlQuery, con)).getOrElse(throw new Exception("Something went wrong"))
                  val queryMetadata = queryResult.resultSet.getMetaData
                  val header = (1 to queryMetadata.getColumnCount).
                    map(i => (
                      queryMetadata.getColumnName(i),
                      (
                        queryMetadata.getColumnType(i),
                        queryResult.resultSet.findColumn(queryMetadata.getColumnName(i))
                      )
                    )).
                    toMap
                  ctx.ctx.currentResultSet = Some((queryResult.resultSet, header))
                  queryResult.rowIndex
                }
              ),
              Field(
                name = s"${schemaTable.tableMetadata.tableName}_aggregate",
                fieldType = schemaTable.aggregatedTable,
                description = Some(s"""fetch aggregated fields from the table: "${schemaTable.tableMetadata.tableName}""""),
                arguments = selectArguments,
                resolve = (ctx: Context[Ctx, Any]) => {
                  val aggregatedQuery = generateAggregatedQuery(schemaTable, ctx)
                  logger.info(aggregatedQuery)
                  val queryResult = (for {
                    con <- ctx.ctx.connection
                  } yield getQueryResult(aggregatedQuery, con)).getOrElse(throw new Exception("Something went wrong"))
                  val resultSet = queryResult.resultSet
                  val _ = resultSet.next()
                  val columnCount = queryResult.resultSet.getMetaData.getColumnCount
                  (1 to columnCount).
                    map(i => (resultSet.getMetaData.getColumnName(i), getRowField(resultSet, i, resultSet.getMetaData.getColumnType(i)))).
                    toMap[String, Any]
                }
              )
            )
          }): _*))

      Schema(query)
    }

}
