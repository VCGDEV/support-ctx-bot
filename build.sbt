import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.example",
      scalaVersion := "2.11.7",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "suppport-ctx-bot",
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
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.3" % Runtime

val droolsVersion = "6.0.1.Final"

resolvers += "JBoss public" at "http://repository.jboss.org/nexus/content/groups/public/"

libraryDependencies ++= {
  "org.kie" % "kie-api" % droolsVersion ::
    List("drools-compiler", "drools-core", "drools-jsr94", "drools-decisiontables", "knowledge-api")
      .map("org.drools" % _ % droolsVersion)
}
