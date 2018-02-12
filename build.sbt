name := "scandroid"
organization := "jorokr21"
version := "0.1"
scalaVersion := "2.12.4"

scalacOptions ++= Seq(
  "-encoding", "UTF-8",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-language:higherKinds",
  "-Ypartial-unification",
  "-Xfuture",
  "-Xlint",
)

val versions = new {
  val akka = "2.5.9"
  val akkaHttp = "10.0.11"
  val akkaHttpCirce = "1.19.0"
  val akkaTyped = "2.5.4"
  val circe = "0.9.1"
  val lucene = "7.2.1"
  val jsoup = "1.8.1"
  val quickLens = "1.4.11"
}

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % versions.akka,
  "com.typesafe.akka" %% "akka-http" % versions.akkaHttp,
  "com.typesafe.akka" %% "akka-stream" % versions.akka,

  "com.typesafe.akka" %% "akka-testkit" % versions.akka % Test,
  "com.typesafe.akka" %% "akka-http-testkit" % versions.akkaHttp % Test,
  "com.typesafe.akka" %% "akka-stream-testkit" % versions.akka % Test,

  "org.apache.lucene" % "lucene-core" % versions.lucene,
  "org.apache.lucene" % "lucene-analyzers-common" % versions.lucene,
  "org.apache.lucene" % "lucene-highlighter" % versions.lucene,
  "org.apache.lucene" % "lucene-queryparser" % versions.lucene,

  "io.circe" %% "circe-generic" % versions.circe,
  "org.jsoup" % "jsoup" % versions.jsoup,
  "com.softwaremill.quicklens" %% "quicklens" % versions.quickLens,
  "de.heikoseeberger" %% "akka-http-circe" % versions.akkaHttpCirce,
)
