import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes

import sbt.ConflictWarning
import sbt.Keys.libraryDependencies

import scala.io.Source
import scala.util.Try

name := "gmql-web"

version := "2.2-SNAPSHOT"

lazy val `gmql-web` = (project in file(".")).enablePlugins(PlayScala, SbtWeb)

scalaVersion := "2.11.8"


libraryDependencies += "com.h2database" % "h2" % "1.4.192"

libraryDependencies ++= Seq(
  cache,
  ws,
  specs2 % Test)


libraryDependencies += "com.typesafe.play" %% "play-slick" % "1.1.1"
libraryDependencies += "com.typesafe.play" %% "play-slick-evolutions" % "1.1.1"


libraryDependencies += "joda-time" % "joda-time" % "2.7"
libraryDependencies += "org.joda" % "joda-convert" % "1.7"
libraryDependencies += "com.github.tototoshi" %% "slick-joda-mapper" % "2.0.0"


unmanagedResourceDirectories in Test <+= baseDirectory(_ / "target/web/public/test")

libraryDependencies += "com.github.scala-incubator.io" %% "scala-io-file" % "0.4.3"

libraryDependencies ++= Seq(
  "org.webjars" %% "webjars-play" % "2.4.0-2"
  , "org.webjars" % "bootstrap" % "3.3.7"
  , "org.webjars" % "jquery" % "2.2.4"
  , "org.webjars" % "jquery-ui" % "1.12.0"
  , "org.webjars" % "bootstrapvalidator" % "0.5.3"
  , "org.webjars" % "js-cookie" % "2.1.0"
  , "org.webjars" % "bootstrap-select" % "1.9.4"
  , "org.webjars" % "jquery-file-upload" % "9.10.1"
  , "org.webjars" % "bootstrap-notify" % "3.1.3"
  , "org.webjars" % "datatables" % "1.10.15"
)


libraryDependencies += "com.jsuereth" %% "scala-arm" % "2.0"
libraryDependencies += "com.typesafe.play" %% "play-mailer" % "4.0.0"



includeFilter in(Assets, LessKeys.less) := "*.less"

excludeFilter in(Assets, LessKeys.less) := "_*.less"


libraryDependencies += "com.sun.jersey.contribs" % "jersey-multipart" % "1.9"
libraryDependencies += "com.sun.jersey" % "jersey-core" % "1.9"
libraryDependencies += "org.eclipse.persistence" % "eclipselink" % "2.6.3"




resolvers += Resolver.mavenLocal
//resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

val gmqlVersion = "1.0-SNAPSHOT"


libraryDependencies ++= Seq(
  "it.polimi.genomics" % "Compiler",
  "it.polimi.genomics" % "GMQL-Core",
  "it.polimi.genomics" % "GMQL-Repository",
  "it.polimi.genomics" % "GMQL-Server",
  "it.polimi.genomics" % "GMQL-SManager",
  "it.polimi.genomics" % "GMQL-Spark"
).map(_ % gmqlVersion)



libraryDependencies += "it.polimi.genomics" % "GMQL-Cli" % gmqlVersion % "provided" classifier "jar-with-dependencies"


routesGenerator := InjectedRoutesGenerator
conflictWarning := ConflictWarning.disable



libraryDependencies += "io.swagger" %% "swagger-play2" % "1.5.1"
libraryDependencies += "io.swagger" % "swagger-core" % "1.5.10"

libraryDependencies += "com.softwaremill.sttp" %% "core" % "1.3.1"


val copyJarsTask = taskKey[Unit]("Copies the required jars to the lib folder")

val gmqlLibPath = "conf/gmql_lib/"

val gmqlVersionPath = "public/GMQL-version.txt"
val gmqlWebVersionPath = "public/GMQL-WEB-version.txt"



copyJarsTask := {
  val jarName = "GMQL-Cli-" + gmqlVersion + "-jar-with-dependencies.jar"

  val folder = new File(gmqlLibPath)
  (managedClasspath in Compile).value.files.foreach(f => {
    if (f.getName == jarName) {
      val inFileAtt = Try(Files.readAttributes(f.toPath, classOf[BasicFileAttributes]))
      val outFileAtt = Try(Files.readAttributes((folder / "GMQL-Cli.jar").toPath, classOf[BasicFileAttributes]))

      //check if the file size and modified dates are same
      if (inFileAtt.map(_.lastModifiedTime) != outFileAtt.map(_.lastModifiedTime)
        || inFileAtt.map(_.size) != outFileAtt.map(_.size)) {
        streams.value.log.info("Copying " + jarName + " to " + gmqlLibPath)
        IO.copyFile(f, folder / "GMQL-Cli.jar", true)
      }
    }
  })

  def getContent(filePath: String): String =
    Try {
      val bufferedSource = Source.fromFile(filePath)
      val contents = bufferedSource.mkString
      bufferedSource.close
      contents
    }.getOrElse("")

  def saveVersion(filePath: String, version: String): Unit = {
    val pwMain = new PrintWriter(new File(filePath))
    pwMain.print(version)
    pwMain.close()
  }

  if (getContent(gmqlVersionPath) != gmqlVersion) {
    streams.value.log.info("Saving gmql version(" + gmqlVersion + ") to " + gmqlVersionPath)
    saveVersion(gmqlVersionPath, gmqlVersion)

  }

  if (getContent(gmqlWebVersionPath) != version.value) {
    streams.value.log.info("Saving gmql-web version(" + version.value + ") to " + gmqlWebVersionPath)
    saveVersion(gmqlWebVersionPath, version.value)
  }
}

compile in Compile <<= (compile in Compile).dependsOn(copyJarsTask)

cleanFiles <+= baseDirectory(_ / gmqlLibPath)

cleanFiles <+= baseDirectory(_ / gmqlVersionPath)
cleanFiles <+= baseDirectory(_ / gmqlWebVersionPath)
