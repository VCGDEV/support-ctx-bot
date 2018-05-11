import Dependencies._
lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.example",
      scalaVersion := "2.11.7",
      version      := "0.0.2-SNAPSHOT"
    )),
    name := "support-ctx-bot",
    libraryDependencies += scalaTest % Test
  )
libraryDependencies += "info.mukel" %% "telegrambot4s" % "3.0.14"
resolvers ++= Seq(Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots"),
  Resolver.bintrayRepo("scalaz", "releases"),
  Resolver.bintrayRepo("megamsys", "scala"))
libraryDependencies += "io.megam" %% "newman" % "1.3.12"
libraryDependencies += "com.typesafe" % "config" % "1.2.1"
libraryDependencies += "net.liftweb" %% "lift-json" % "2.6-M4"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"
val droolsVersion = "6.1.0.Final"

resolvers += "JBoss public" at "http://repository.jboss.org/nexus/content/groups/public/"

libraryDependencies ++= {
  "org.kie" % "kie-api" % droolsVersion ::
    List("drools-compiler", "drools-core", "drools-jsr94", "drools-decisiontables", "knowledge-api")
      .map("org.drools" % _ % droolsVersion)
}

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % "1.7.5",
"com.typesafe.slick" %% "slick" % "3.2.3",
"org.postgresql" % "postgresql" % "9.4.1212",
"com.h2database" % "h2" % "1.4.196" % "test",
"com.typesafe.slick" %% "slick-hikaricp" % "3.2.3",
  "javax.mail" % "mail" % "1.4"
)
assemblyMergeStrategy in assembly := {
  case PathList("javax", "servlet", xs @ _*)         => MergeStrategy.first
  case PathList("org", "xmlpull", xs @ _*)         => MergeStrategy.last
  case PathList(ps @ _*) if ps.last endsWith ".html" => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".xls" => MergeStrategy.discard
  case "application.conf"                            => MergeStrategy.concat
  case "unwanted.txt"                                => MergeStrategy.discard
  case "kmodule.xml"                                     => MergeStrategy.concat
  case "META-INF/maven/pom.properties"                                     => MergeStrategy.concat
  case "org/drools/compiler/lang/dsl/rule1.drl" => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}