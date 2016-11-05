import sbt._

object Dependencies {

  object Version {
    val akka = "2.4.8"
    val phantom = "1.29.5"
  }

  lazy val model = common ++ db ++ tests

  val common = Seq(
    "com.typesafe" % "config" % "1.3.1"
  )

  val db = Seq(
    "com.websudos"  %% "phantom-connectors" % Version.phantom,
    "com.websudos"  %% "phantom-dsl" % Version.phantom,
    "com.websudos"  %% "phantom-example" % Version.phantom,
    "com.websudos"  %% "phantom-finagle" % Version.phantom,
    "com.websudos"  %% "phantom-jdk8" % Version.phantom,
    "com.websudos"  %% "phantom-thrift" % Version.phantom,
    "com.websudos"  %% "phantom-reactivestreams" % Version.phantom,
    "com.websudos" %% "util-testing" % "0.13.0" % "test, provided"
  )

  val akka = Seq(
    "com.typesafe.akka" %% "akka-actor" % Version.akka,
    "com.typesafe.akka" %% "akka-testkit" % Version.akka % Test,
    "com.typesafe.akka" %% "akka-slf4j" % Version.akka
  )

  val tests = Seq(
    "org.scalatest" %% "scalatest" % "2.2.6" % Test
  )

}