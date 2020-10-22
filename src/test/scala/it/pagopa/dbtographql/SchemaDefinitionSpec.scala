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

package it.pagopa.dbtographql

import java.sql.DriverManager

import io.circe.Json
import it.pagopa.dbtographql.database.{DatabaseDataMgmt, DatabaseMetadataMgmt}
import it.pagopa.dbtographql.schema.{Ctx, SchemaDefinition}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import sangria.execution.Executor
import sangria.macros.LiteralGraphQLStringContext
import sangria.marshalling.circe._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

@SuppressWarnings(
  Array(
    "org.wartremover.warts.Equals",
    "org.wartremover.warts.Var",
    "org.wartremover.warts.Any",
    "org.wartremover.warts.Nothing",
    "org.wartremover.warts.Throw",
    "org.wartremover.warts.AnyVal",
    "org.wartremover.warts.TraversableOps",
    "org.wartremover.warts.AsInstanceOf"
  )
)
class SchemaDefinitionSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll with DatabaseMetadataMgmt with DatabaseDataMgmt with SchemaDefinition {

  locally {
    val _ = Class.forName("org.h2.Driver")
  }

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  val DATABASE_URL: String = "jdbc:h2:mem:db1"

  protected def generateToken(username: String, password: String, database: String): String = ???
  protected def getConnection(uri: String, username: String, password: String): java.sql.Connection = ???

  "The publisher" must {
    "support the comparison operators for all the numerical types" in {
      val connection = DriverManager.getConnection(DATABASE_URL)
      val stm = connection.createStatement
      val _ = stm.execute(
        """create schema `test`;
        """.stripMargin
      )
      val _ = stm.execute(
        """
          |create table test.test_numeric(
          |   DECIMAL_TYPE     DECIMAL,
          |   NUMERIC_TYPE     NUMERIC,
          |   DOUBLE_TYPE      DOUBLE,
          |   REAL_TYPE        REAL,
          |   FLOAT_TYPE       FLOAT,
          |   BIGINT_TYPE      BIGINT,
          |   INTEGER_TYPE     INTEGER,
          |   SMALLINT_TYPE    SMALLINT,
          |   TINYINT_TYPE     TINYINT
          |   );
          |   insert into test.test_numeric values (0.01, 0.02, 0.03, 0.04, 0.05, 100, 200, 10, 1);
          |   insert into test.test_numeric values (0.02, 0.03, 0.04, 0.05, 0.06, 101, 201, 11, 2);
          |   insert into test.test_numeric values (0.03, 0.04, 0.05, 0.06, 0.07, 102, 202, 12, 3);
          |   """.stripMargin
      )
      val databaseMetadata = getDatabaseMetadata(connection, "TEST")
      val schema = generateSchema(databaseMetadata)

      { // DECIMAL TYPE
        val query = graphql"""
           query{
              TEST_NUMERIC(where: { DECIMAL_TYPE: {_lt: 0.03} }){
                 DECIMAL_TYPE
               }
             }
          """
        val result: Json = Await.result(Executor.execute(schema, query, Ctx(connection = Some(connection))), Duration.Inf)
        ((result \\ "data").head \\ "TEST_NUMERIC").flatMap(j => j \\ "DECIMAL_TYPE").map(_.as[BigDecimal]).map(_.getOrElse(BigDecimal("0"))) must be(List(BigDecimal("0.01"), BigDecimal(0.02)))
      }

      { // NUMERIC TYPE
        val query = graphql"""
             query{
               TEST_NUMERIC(where: { NUMERIC_TYPE: {_lt: 0.04} }){
                 NUMERIC_TYPE
               }
             }
          """
        val result: Json = Await.result(Executor.execute(schema, query, Ctx(connection = Some(connection))), Duration.Inf)
        ((result \\ "data").head \\ "TEST_NUMERIC").flatMap(j => j \\ "NUMERIC_TYPE").map(_.as[BigDecimal]).map(_.getOrElse(BigDecimal("0"))) must be(List(BigDecimal("0.02"), BigDecimal(0.03)))
      }

      { // DOUBLE TYPE
        val query = graphql"""
             query{
               TEST_NUMERIC(where: { DOUBLE_TYPE: {_lt: 0.05} }){
                 DOUBLE_TYPE
               }
             }
          """
        val result: Json = Await.result(Executor.execute(schema, query, Ctx(connection = Some(connection))), Duration.Inf)
        val values = ((result \\ "data").head \\ "TEST_NUMERIC").flatMap(j => j \\ "DOUBLE_TYPE").map(_.as[Double]).map(_.getOrElse(0d))
        val _ = values.head must be(0.03d +- 0.0000000001)
        val _ = values.tail.head must be(0.04d +- 0.0000000001)
      }

      { // REAL TYPE
        val query = graphql"""
             query{
               TEST_NUMERIC(where: { REAL_TYPE: {_lt: 0.06} }){
                 REAL_TYPE
               }
             }
             """
        val result: Json = Await.result(Executor.execute(schema, query, Ctx(connection = Some(connection))), Duration.Inf)
        val values = ((result \\ "data").head \\ "TEST_NUMERIC").flatMap(j => j \\ "REAL_TYPE").map(_.as[Double]).map(_.getOrElse(0d))
        val _ = values.head must be(0.04d +- 0.000000001)
        val _ = values.tail.head must be(0.05d +- 0.000000001)
      }

      { // FLOAT TYPE
        val query = graphql"""
             query{
               TEST_NUMERIC(where: { FLOAT_TYPE: {_lt: 0.07} }){
                 FLOAT_TYPE
               }
             }
             """
        val result: Json = Await.result(Executor.execute(schema, query, Ctx(connection = Some(connection))), Duration.Inf)
        val values = ((result \\ "data").head \\ "TEST_NUMERIC").flatMap(j => j \\ "FLOAT_TYPE").map(_.as[Float]).map(_.getOrElse(0f))
        val _ = values.head must be(0.05f +- 0.00000001f)
        val _ = values.tail.head must be(0.06f +- 0.00000001f)
      }

      { // BIGINT TYPE
        val query = graphql"""
             query{
               TEST_NUMERIC(where: { BIGINT_TYPE: {_lt: 102} }){
                 BIGINT_TYPE
               }
             }
          """
        val result: Json = Await.result(Executor.execute(schema, query, Ctx(connection = Some(connection))), Duration.Inf)
        ((result \\ "data").head \\ "TEST_NUMERIC").flatMap(j => j \\ "BIGINT_TYPE").map(_.as[Long]).map(_.getOrElse(0L)) must be(List(100L, 101L))
      }

      { // INTEGER TYPE
        val query = graphql"""
             query{
               TEST_NUMERIC(where: { INTEGER_TYPE: {_lt: 202} }){
                 INTEGER_TYPE
               }
             }
          """
        val result: Json = Await.result(Executor.execute(schema, query, Ctx(connection = Some(connection))), Duration.Inf)
        ((result \\ "data").head \\ "TEST_NUMERIC").flatMap(j => j \\ "INTEGER_TYPE").map(_.as[Long]).map(_.getOrElse(0)) must be(List(200, 201))
      }

      { // SMALLINT TYPE
        val query = graphql"""
             query{
               TEST_NUMERIC(where: { SMALLINT_TYPE: {_lt: 12} }){
                 SMALLINT_TYPE
               }
             }
             """
        val result: Json = Await.result(Executor.execute(schema, query, Ctx(connection = Some(connection))), Duration.Inf)
        ((result \\ "data").head \\ "TEST_NUMERIC").flatMap(j => j \\ "SMALLINT_TYPE").map(_.as[Int]).map(_.getOrElse(0)) must be(List(10, 11))
      }

      { // TINYINT TYPE
        val query = graphql"""
             query{
               TEST_NUMERIC(where: { TINYINT_TYPE: {_lt: 3} }){
                 TINYINT_TYPE
               }
             }
             """
        val result: Json = Await.result(Executor.execute(schema, query, Ctx(connection = Some(connection))), Duration.Inf)
        ((result \\ "data").head \\ "TEST_NUMERIC").flatMap(j => j \\ "TINYINT_TYPE").map(_.as[Int]).map(_.getOrElse(0)) must be(List(1, 2))
      }

      connection.close()
    }
  }

  "The publisher" must {
    "support the comparison operators the BOOLEAN type" in {
      val connection = DriverManager.getConnection(DATABASE_URL)
      val stm = connection.createStatement
      val _ = stm.execute(
        """create schema `test`;
        """.stripMargin
      )
      val _ = stm.execute(
        """
          |create table test.test_boolean(
          |   BOOLEAN_TYPE BOOLEAN
          |   );
          |   insert into test.test_boolean values (true);
          |   insert into test.test_boolean values (false);
          |   insert into test.test_boolean values (false);
          |   """.stripMargin
      )
      val databaseMetadata = getDatabaseMetadata(connection, "TEST")
      val schema = generateSchema(databaseMetadata)

      val query = graphql"""
           query{
             TEST_BOOLEAN(where: { BOOLEAN_TYPE: {_eq: true} }){
               BOOLEAN_TYPE
             }
           }
           """
      val result: Json = Await.result(Executor.execute(schema, query, Ctx(connection = Some(connection))), Duration.Inf)
      val values = ((result \\ "data").head \\ "TEST_BOOLEAN").flatMap(j => j \\ "BOOLEAN_TYPE").map(_.as[Boolean]).map(_.getOrElse(false))
      val _ = values.length must be(1)

      connection.close()
    }
  }

  "The publisher" must {
    "support the comparison operators for the TIMESTAMP type" in {
      val connection = DriverManager.getConnection(DATABASE_URL)
      val stm = connection.createStatement
      val _ = stm.execute(
        """create schema `test`;
        """.stripMargin
      )
      val _ = stm.execute(
        """
          |create table test.test_time(
          |   TIMESTAMP_TYPE TIMESTAMP
          |   );
          |   insert into test.test_time values ('2001-01-09 01:05:01');
          |   insert into test.test_time values ('2001-01-09 01:06:01');
          |   insert into test.test_time values ('2001-01-09 01:07:01');
          |   """.stripMargin
      )
      val databaseMetadata = getDatabaseMetadata(connection, "TEST")
      val schema = generateSchema(databaseMetadata)
      val query =
        graphql"""
          query{
              TEST_TIME(where: { TIMESTAMP_TYPE: {_lt: "2001-01-09 01:07:01"} }){
                 TIMESTAMP_TYPE
              }
          }
          """
      val result: Json = Await.result(Executor.execute(schema, query, Ctx(connection = Some(connection))), Duration.Inf)
      val values = ((result \\ "data").head \\ "TEST_TIME").flatMap(j => j \\ "TIMESTAMP_TYPE").map(_.as[String]).map(_.getOrElse(false))
      val _ = values.length must be(2)
      connection.close()
    }
  }

  "The publisher" must {
    "support the comparison operators for the DATE type" in {
      val connection = DriverManager.getConnection(DATABASE_URL)
      val stm = connection.createStatement
      val _ = stm.execute(
        """create schema `test`;
        """.stripMargin
      )
      val _ = stm.execute(
        """
          |create table test.test_date(
          |   DATE_TYPE DATE
          |   );
          |   insert into test.test_date values ('2001-01-09');
          |   insert into test.test_date values ('2001-01-10');
          |   insert into test.test_date values ('2001-01-11');
          |   """.stripMargin
      )
      val databaseMetadata = getDatabaseMetadata(connection, "TEST")
      val schema = generateSchema(databaseMetadata)
      val query =
        graphql"""
          query{
              TEST_DATE(where: { DATE_TYPE: {_lt: "2001-01-11"} }){
                 DATE_TYPE
              }
          }
          """
      val result: Json = Await.result(Executor.execute(schema, query, Ctx(connection = Some(connection))), Duration.Inf)
      val values = ((result \\ "data").head \\ "TEST_DATE").flatMap(j => j \\ "DATE_TYPE").map(_.as[String]).map(_.getOrElse(false))
      val _ = values.length must be(2)
      connection.close()
    }
  }
}
