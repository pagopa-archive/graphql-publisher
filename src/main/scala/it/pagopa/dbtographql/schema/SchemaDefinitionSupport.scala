package it.pagopa.dbtographql.schema

import it.pagopa.dbtographql.database.DatabaseMetadataMgmt
import it.pagopa.dbtographql.database.DatabaseMetadataModel.TableMetadata
import sangria.schema.{Argument, BooleanType, Context, EnumType, EnumValue, Field, FloatType, LongType, ObjectType, OptionInputType, ScalarType}

@SuppressWarnings(
  Array(
    "org.wartremover.warts.Any",
    "org.wartremover.warts.Nothing"
  )
)
trait SchemaDefinitionSupport extends SchemaDefinitionSupportCommons with DatabaseMetadataMgmt {

  protected sealed trait AggregationOperation

  protected final case object Avg extends AggregationOperation

  protected final case object Max extends AggregationOperation

  protected final case object Min extends AggregationOperation

  protected final case object Count extends AggregationOperation

  protected def tableSelectColumn(table: TableMetadata): EnumType[String] = EnumType(s"${table.tableName}_select_column", Some(s"""select columns of table "${table.tableName}""""), table.columns.map(c => EnumValue(c.columnName, Some("column name"), c.columnName)))

  protected def getAggregateFieldType(table: TableMetadata, aggregationOperation: AggregationOperation): Option[Field[Ctx, Any]] =
    aggregationOperation match {
      case Avg =>
        val avgColumns = table.columns.filter(c => isNumericField(c.columnType))
        if (avgColumns.isEmpty)
          None: Option[Field[Ctx, Any]]
        else
          Some(
            Field[Ctx, Any, Any, Any](
              "avg",
              ObjectType(
                s"${table.tableName}_avg_fields",
                "aggregate avg on columns",
                avgColumns.map(c => {
                  Field(
                    c.columnName,
                    FloatType.asInstanceOf[ScalarType[Any]],
                    resolve = (ctx: Context[Ctx, Any]) => ctx.value.asInstanceOf[Map[String, Any]].apply(s"avg(${c.columnName})")
                  )
                })
              ),
              resolve = (ctx: Context[Ctx, Any]) => ctx.value
            )
          )
      case Max =>
        Some(
          Field[Ctx, Any, Any, Any](
            "max",
            ObjectType(
              s"${table.tableName}_max_fields",
              "aggregate max on columns",
              table.columns.map(c => {
                Field[Any, Any, Any, Any](
                  c.columnName,
                  getFieldType(c.columnType),
                  resolve = (ctx: Context[Any, Any]) => ctx.value.asInstanceOf[Map[String, Any]].apply(s"max(${c.columnName})")
                )
              })
            ),
            resolve = (ctx: Context[Ctx, Any]) => ctx.value
          )
        )
      case Min =>
        Some(
          Field[Ctx, Any, Any, Any](
            "min",
            ObjectType(
              s"${table.tableName}_min_fields",
              "aggregate min on columns",
              table.columns.map(c => {
                Field[Any, Any, Any, Any](
                  c.columnName,
                  getFieldType(c.columnType),
                  resolve = (ctx: Context[Any, Any]) => ctx.value.asInstanceOf[Map[String, Any]].apply(s"min(${c.columnName})")
                )
              })
            ),
            resolve = (ctx: Context[Ctx, Any]) => ctx.value
          )
        )
      case Count =>
        Some(
          Field[Ctx, Any, Any, Any](
            name = "count",
            fieldType = LongType,
            arguments = Argument("columns", tableSelectColumn(table), "") :: Argument("distinct", OptionInputType(BooleanType), "") :: Nil: List[Argument[_]],
            resolve = (ctx: Context[Ctx, Any]) => {
              val columName = ctx.args.arg[String]("columns")
              val columnNameWithDistinct = s"distinct $columName"
              val map = ctx.value.asInstanceOf[Map[String, Any]]
              val value = map.getOrElse(s"count($columName)", map(s"count($columnNameWithDistinct)"))
              value
            }
          )
        )

    }

}
