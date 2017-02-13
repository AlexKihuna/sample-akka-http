name := "network"

version := "1.0"

scalaVersion := "2.12.1"

scalacOptions := Seq(
  "-deprecation",
  "-unchecked"
)

val scalaTestVersion = "3.0.1"
val akkaHttpVersion = "10.0.3"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-testkit"    % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "org.scalactic"     %% "scalactic"            % scalaTestVersion ,
  "org.scalatest"     %% "scalatest"            % scalaTestVersion % "test"
)