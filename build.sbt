import AssemblyKeys._ 

assemblySettings

name  := "musicrest-2.10"

version := "1.1.5"

jarName in assembly <<= (name, version) map { (n, v) => n + "-" + v + ".jar" }

organization  := "org.bayswater.musicrest"

scalaVersion  := "2.10.2"


scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

resolvers ++= Seq(
  "spray repo" at "http://repo.spray.io/",
  // for temporary Casbah
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
)

libraryDependencies ++= Seq(
  "io.spray"            %   "spray-can"          % "1.1.0",
  "io.spray"            %   "spray-routing"      % "1.1.0",
  "io.spray"            %   "spray-caching"      % "1.1.0",
  "io.spray"            %   "spray-testkit"      % "1.1.0",
  "com.typesafe.akka"   %%  "akka-actor"         % "2.1.4",
  "com.typesafe.akka"   %%  "akka-testkit"       % "2.1.4",
  "org.scalaz"          %   "scalaz-core_2.10"   % "7.0.0",
  "org.mongodb"         %%  "casbah"             % "2.6.2",
  "net.liftweb"         %%  "lift-json"          % "2.5",
  "javax.mail"          %   "mail"               % "1.4",
  "org.specs2"          %%  "specs2"             % "1.14" % "test"
)

fork := true

javaOptions in run += "-Dconfig.file=/home/john/Development/Workspace/Spray/musicrest/conf/musicrest.conf"

javaOptions in test += "-Dconfig.file=/home/john/Development/Workspace/Spray/musicrest/conf/test.conf"

net.virtualvoid.sbt.graph.Plugin.graphSettings

seq(Revolver.settings: _*)
