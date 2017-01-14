lazy val root = (project in file("."))
  .settings(
    name := """xmlarchiveparser-testtask"""
  ).aggregate(
    model
  )

val commonSettings = Seq(
  organization := "otokarev@gmail.com",
  version := "0.1.0",
  scalaVersion := "2.11.8",
  resolvers ++= Seq(
    "Java.net Maven2 Repository" at "http://download.java.net/maven/2/",
    "Twitter Repository" at "http://maven.twttr.com",
    Resolver.typesafeRepo("releases"),
    Resolver.sonatypeRepo("releases")
  )
)

lazy val model = (project in file("model"))
  .settings(
    name := "xmlarchiveparser-testtask-model",
    libraryDependencies ++= Dependencies.model,
    fork in run := true,
    fork in Test := false,
    Defaults.coreDefaultSettings ++ commonSettings ++ PhantomSbtPlugin.projectSettings
  )

