name := "drudgescraper"

version := "0.0.1"

scalaVersion := "2.12.2"

scalacOptions += "-feature" // for useful warnings

resolvers += "Twitter Maven Repo" at "http://maven.twttr.com/"
resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

lazy val akkaVersion = "2.5.11"
lazy val akkaHttpVersion= "10.1.0"

libraryDependencies ++= Seq(
  "org.jsoup" % "jsoup" % "1.11.2",
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"
)

scalacOptions += "-deprecation"


