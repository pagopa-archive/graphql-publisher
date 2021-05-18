import Dependencies._

ThisBuild / scalaVersion := "2.13.6"
ThisBuild / organization := "it.pagopa"
ThisBuild / organizationName := "Pagopa S.p.A."
ThisBuild / wartremoverErrors ++= Warts.all
ThisBuild / parallelExecution in Test := false
ThisBuild / fork in Test := false
ThisBuild / resolvers += "Cloudera Repo" at "https://repository.cloudera.com/artifactory/cloudera-repos/"
ThisBuild / startYear := Some(2020)
ThisBuild / licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt"))

Docker / packageName := "services/graphql-publisher"
Docker / daemonUser := "daemon"
Docker / dockerRepository := Some(System.getenv("DOCKER_REPO"))
dockerExposedPorts := Seq(8080)
dockerBaseImage := "openjdk:8-jre-alpine"
dockerUpdateLatest := true

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-explaintypes",
  "-Yrangepos",
  "-feature",
  "-language:higherKinds",
  "-language:existentials",
  "-unchecked",
  "-Xlint:_,-type-parameter-shadow",
  "-Xfatal-warnings",
  "-Ywarn-numeric-widen",
  "-Ywarn-unused:patvars,-implicits",
  "-Ywarn-value-discard"
)

javacOptions ++= Seq(
  "-Xlint:unchecked"
)

lazy val root = (project in file("."))
  .settings(
    name := "graphql-publisher",
    libraryDependencies ++= Seq(
      mustache % Compile,
      bck % Compile,
      jwt % Compile,
      catsCore % Compile,
      catsKernel % Compile,
      catsFree % Compile,
      catsMacros % Compile,
      sangria % Compile,
      sangriaCirce % Compile,
      akkaStream % Compile,
      akkaHttp % Compile,
      akkaHttpCirce % Compile,
      circeCore % Compile,
      circeParser % Compile,
      circeOptics % Compile,
      hiveJdbc % Compile,
      scalaTest % Test,
      h2Db % Test
    ) ++ loggingDependencies.map(dep => dep % Compile)
  ).enablePlugins(AshScriptPlugin, DockerPlugin, AutomateHeaderPlugin)
