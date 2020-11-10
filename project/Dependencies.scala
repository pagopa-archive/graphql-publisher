import sbt._

object Dependencies {
  lazy val mustache = "com.github.spullara.mustache.java" % "compiler" % "0.9.7"
  lazy val bck = "org.bouncycastle" % "bcprov-jdk15on" % "1.67"
  lazy val jwt = "com.auth0" % "java-jwt" % "3.11.0"
  lazy val catsCore = "org.typelevel" %% "cats-core" % "2.2.0"
  lazy val catsKernel = "org.typelevel" %% "cats-kernel" % "2.2.0"
  lazy val catsFree = "org.typelevel" %% "cats-free" % "2.2.0"
  lazy val catsMacros = "org.typelevel" %% "cats-macros" % "2.1.1"
  lazy val sangria = "org.sangria-graphql" %% "sangria" % "2.0.1"
  lazy val sangriaCirce = "org.sangria-graphql" %% "sangria-circe" % "1.3.1"
  lazy val akkaHttp = "com.typesafe.akka" %% "akka-http" % "10.2.1"
  lazy val akkaStream = "com.typesafe.akka" %% "akka-stream" % "2.6.10"
  lazy val akkaHttpCirce = "de.heikoseeberger" %% "akka-http-circe" % "1.35.2"
  lazy val circeCore = "io.circe" %% "circe-core" % "0.13.0"
  lazy val circeParser = "io.circe" %% "circe-parser" % "0.13.0"
  lazy val circeOptics = "io.circe" %% "circe-optics" % "0.13.0"
  lazy val hiveJdbc = "org.apache.hive" % "hive-jdbc" % "3.1.3000.7.2.2.0-244" exclude("org.slf4j", "slf4j-log4j12") exclude("org.apache.logging.log4j", "log4j-slf4j-impl") exclude("org.apache.logging.log4j", "log4j-1.2-api")

  lazy val loggingDependencies = Seq(
    "org.apache.logging.log4j" % "log4j-api" % "2.14.0",
    "org.apache.logging.log4j" % "log4j-core" % "2.14.0",
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "org.slf4j" % "slf4j-api" % "1.7.30"
  )

  lazy val h2Db = "com.h2database" % "h2" % "1.4.200"
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.3"
}
