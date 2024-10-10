
name  := "musicrest-2.11"

version := "1.3.3"

organization  := "org.bayswater.musicrest"

scalaVersion  := "2.11.12"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8", "-feature")

resolvers ++= Seq(
   ("spray repo" at 
  "http://repo.spray.io/").withAllowInsecureProtocol(true),
  // for temporary Casbah
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
)


libraryDependencies ++= Seq(
  "io.spray"               %   "spray-can_2.11"           % "1.3.4",
  "io.spray"               %   "spray-routing_2.11"       % "1.3.4",
  "io.spray"               %   "spray-caching_2.11"       % "1.3.4",
  "io.spray"               %   "spray-testkit_2.11"       % "1.3.4",
  "com.typesafe.akka"      %%  "akka-actor"               % "2.4.11",
  "com.typesafe.akka"      %%  "akka-testkit"             % "2.4.11",
  "org.scala-lang.modules" %%  "scala-parser-combinators" % "1.0.4",
  "org.scalaz"             %%  "scalaz-core"              % "7.1.6",
  "org.scalaz.stream"      %%  "scalaz-stream"            % "0.8",
  "org.mongodb"            %%  "casbah"                   % "3.1.1",
  "net.liftweb"            %   "lift-webkit_2.11"         % "3.0-M8",
  "javax.mail"             %   "mail"                     % "1.4.7",
  "org.specs2"             %%  "specs2"                   % "2.5-scalaz-7.1.6" % "test",
  "io.argonaut"            %%  "argonaut"                 % "6.1" % "test"
)

fork := true

javaOptions in run += "-Dconfig.file=/home/john/Development/Workspace/Spray/musicrest/conf/musicrest.conf"

javaOptions in Test += "-Dconfig.file=/home/john/Development/Workspace/Spray/musicrest/conf/test.conf"

scalacOptions in Test ++= Seq("-Yrangepos")

assembly / test := {}
