name := "auth-plugin"

scalaVersion := "2.11.7"
version := "1.0"

resolvers += "Mesosphere Public Repo" at "http://downloads.mesosphere.io/maven"

libraryDependencies ++= Seq(
  "mesosphere.marathon" %% "plugin-interface" % "1.4.2" % "provided",
  "log4j" % "log4j" % "1.2.17" % "provided",
  "org.mindrot" % "jbcrypt" % "0.3m",
  "commons-io" % "commons-io" % "2.6"
)
