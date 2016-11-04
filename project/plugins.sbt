def outworkersPattern = {
  val pList = List("[organisation]/[module](_[scalaVersion])(_[sbtVersion])/[revision]/[artifact]-[revision](-[classifier]).[ext]")
  Patterns(pList, pList, isMavenCompatible = true)
}

resolvers ++= Seq(
  Resolver.url("Maven Ivy Outworkers", url(Resolver.DefaultMavenRepositoryRoot))(outworkersPattern)
)

addSbtPlugin("com.websudos" %% "phantom-sbt" % "1.22.0")