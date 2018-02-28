name := "drudgescraper"

version := "0.0.1"

scalaVersion := "2.12.2"

resolvers += "Twitter Maven Repo" at "http://maven.twttr.com/"
resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

lazy val akkaVersion = "2.5.9"
lazy val akkaHttpVersion= "10.0.11"

libraryDependencies ++= Seq(
  "org.jsoup" % "jsoup" % "1.11.2",
  "codes.reactive" %% "scala-time" % "0.4.1",
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  

  "org.apache.parquet" % "parquet-avro" % "1.9.0",
  "org.apache.hadoop" % "hadoop-common" % "2.7.1",

  "codes.reactive" %% "scala-time" % "0.4.1"
)

scalacOptions += "-deprecation"


