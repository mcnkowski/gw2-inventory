name := "gw2_api_app"

version := "0.1"

scalaVersion := "2.13.8"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.9.2",
  "com.typesafe.akka" %% "akka-http" % "10.2.9",
  "com.typesafe.akka" %% "akka-actor" % "2.6.19",
  "com.typesafe.akka" % "akka-actor-typed_2.13" % "2.6.19",
  "com.typesafe.akka" %% "akka-stream" % "2.6.19",
  "org.scalafx" %% "scalafx" % "18.0.1-R28"
)

idePackagePrefix := Some("com.mcnkowski.gw2")
