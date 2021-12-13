import sbt._

object Dependencies {
  lazy val mustache      = "com.github.spullara.mustache.java" % "compiler"        % "0.9.10"
  lazy val bck           = "org.bouncycastle"                  % "bcprov-jdk15on"  % "1.69"
  lazy val jwt           = "com.auth0"                         % "java-jwt"        % "3.18.2"
  lazy val catsCore      = "org.typelevel"                    %% "cats-core"       % "2.6.1"
  lazy val catsKernel    = "org.typelevel"                    %% "cats-kernel"     % "2.6.1"
  lazy val catsFree      = "org.typelevel"                    %% "cats-free"       % "2.6.1"
  lazy val catsMacros    = "org.typelevel"                    %% "cats-macros"     % "2.1.1"
  lazy val sangria       = "org.sangria-graphql"              %% "sangria"         % "2.1.5"
  lazy val sangriaCirce  = "org.sangria-graphql"              %% "sangria-circe"   % "1.3.2"
  lazy val akkaHttp      = "com.typesafe.akka"                %% "akka-http"       % "10.2.7"
  lazy val akkaStream    = "com.typesafe.akka"                %% "akka-stream"     % "2.6.17"
  lazy val akkaHttpCirce = "de.heikoseeberger"                %% "akka-http-circe" % "1.38.2"
  lazy val circeCore     = "io.circe"                         %% "circe-core"      % "0.14.1"
  lazy val circeParser   = "io.circe"                         %% "circe-parser"    % "0.14.1"
  lazy val circeOptics   = "io.circe"                         %% "circe-optics"    % "0.14.1"

  lazy val hiveJdbc = ("org.apache.hive" % "hive-jdbc" % "3.1.3000.7.2.12.0-291").exclude("org.slf4j", "slf4j-log4j12")
    .exclude("org.apache.logging.log4j", "log4j-slf4j-impl").exclude("org.apache.logging.log4j", "log4j-1.2-api")
    .exclude("log4j", "log4j")

  lazy val loggingDependencies = Seq(
    "org.apache.logging.log4j" % "log4j-api"       % "2.15.0",
    "org.apache.logging.log4j" % "log4j-web"       % "2.15.0",
    "org.apache.logging.log4j" % "log4j-core"      % "2.15.0",
    "ch.qos.logback"           % "logback-classic" % "1.2.6",
    "org.slf4j"                % "slf4j-api"       % "1.7.32"
  )

  lazy val h2Db      = "com.h2database" % "h2"        % "1.4.200"
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.10"
}
