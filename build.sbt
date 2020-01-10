import com.typesafe.sbt.packager.docker.{Cmd, ExecCmd}

import scala.io.Source
import scala.util.parsing.json.JSON

organization := "codacy"

name := "codacy-staticcheck"

version := "1.0-SNAPSHOT"

val languageVersion = "2.12.7"

scalaVersion := languageVersion

libraryDependencies ++= Seq(
  "com.codacy" %% "codacy-engine-scala-seed" % "3.1.0" withSources (),
  "org.scala-lang.modules" %% "scala-xml" % "1.2.0" withSources ()
)

enablePlugins(JavaAppPackaging)

enablePlugins(DockerPlugin)

version in Docker := "1.0"

organization := "com.codacy"

lazy val toolVersion = taskKey[String]("Retrieve the version of the underlying tool from patterns.json")

toolVersion := {
  val jsonFile = (resourceDirectory in Compile).value / "docs" / "patterns.json"
  val toolMap = JSON
    .parseFull(Source.fromFile(jsonFile).getLines().mkString)
    .getOrElse(throw new Exception("patterns.json is not a valid json"))
    .asInstanceOf[Map[String, String]]
  toolMap.getOrElse[String]("version", throw new Exception("Failed to retrieve 'version' from patterns.json"))
}

def installAll(toolVersion: String) =
  s"""apk --no-cache add bash git go musl-dev &&
     |export GOPATH=/opt/docker/go &&
     |go get -u honnef.co/go/tools/cmd/staticcheck &&
     |(cd $$GOPATH/src/honnef.co/go/tools && git checkout $toolVersion) &&
     |go get honnef.co/go/tools/cmd/staticcheck &&
     |rm -rf /usr/lib/go/pkg &&
     |rm -rf $$GOPATH/pkg &&
     |rm -rf $$GOPATH/src &&
     |apk del git musl-dev &&
     |rm -rf /var/cache/apk/* &&
     |rm -rf /tmp/*""".stripMargin.replaceAll(System.lineSeparator(), " ")

mappings in Universal ++= (resourceDirectory in Compile).map { resourceDir =>
  val src = resourceDir / "docs"
  val dest = "/docs"

  for {
    path <- src.allPaths.get if !path.isDirectory
  } yield path -> path.toString.replaceFirst(src.toString, dest)
}.value

val dockerUser = "docker"
val dockerGroup = "docker"

daemonUser in Docker := dockerUser

daemonGroup in Docker := dockerGroup

dockerBaseImage := "openjdk:8-jre-alpine"

mainClass in Compile := Some("codacy.Engine")

dockerCommands := {
  dockerCommands.dependsOn(toolVersion).value.flatMap {
    case cmd @ (Cmd("ADD", _)) =>
      List(
        Cmd("RUN", "adduser -u 2004 -D docker"),
        cmd,
        Cmd("RUN", installAll(toolVersion.value)),
        Cmd("RUN", "mv /opt/docker/docs /docs"),
        Cmd("RUN", s"chown -R $dockerUser:$dockerGroup /docs")
      )
    case other => List(other)
  }
}
