scalaVersion in ThisBuild := "2.11.4"

lazy val loader = (project in file("loader"))
  .enablePlugins(PlayScala)
  .settings(
    libraryDependencies ++= Seq(
      "org.elasticsearch" % "elasticsearch" % "1.4.3",
      "net.sf.supercsv" % "super-csv" % "2.2.0",
      "com.amazonaws" % "aws-java-sdk" % "1.8.7",
      "joda-time" % "joda-time" % "2.4",
      "com.google.guava" % "guava" % "15.0",
      "org.slf4j" % "slf4j-api" % "1.7.7",
      "org.joda" % "joda-convert" % "1.7" % "provided",
      "org.slf4j" % "slf4j-simple" % "1.7.7",
      "com.typesafe.play" %% "play-test" % play.core.PlayVersion.current
      //      "org.scalatest" %% "scalatest" % "2.2.1" % "test"
    )
  )
