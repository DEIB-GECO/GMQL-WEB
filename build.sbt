import org.apache.commons.io.FileUtils
import sbt.ConflictWarning
import sbt.Keys.libraryDependencies

import scala.xml.transform.{RewriteRule, RuleTransformer}
import scala.xml.{Elem, Node, Text, XML}

name := "gmql-web"

version := "1.0-SNAPSHOT"

lazy val `gmql-web` = (project in file(".")).enablePlugins(PlayScala, SbtWeb)

scalaVersion := "2.11.8"


//updated to last version of h2
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

//resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

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




//resolvers += Resolver.mavenLocal
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

val gmql_version = "1.0-SNAPSHOT"


libraryDependencies ++= Seq(
  "it.polimi.genomics" % "Compiler",
  "it.polimi.genomics" % "GMQL-Core",
  "it.polimi.genomics" % "GMQL-Repository",
  "it.polimi.genomics" % "GMQL-Server",
  "it.polimi.genomics" % "GMQL-SManager",
  "it.polimi.genomics" % "GMQL-Spark"
).map(_ % gmql_version)



libraryDependencies += "it.polimi.genomics" % "GMQL-Cli" % gmql_version % "provided" classifier "jar-with-dependencies"


routesGenerator := InjectedRoutesGenerator
conflictWarning := ConflictWarning.disable



libraryDependencies += "io.swagger" %% "swagger-play2" % "1.5.1"
libraryDependencies += "io.swagger" % "swagger-core" % "1.5.10"


val copyJarsTask = taskKey[Unit]("Copies the required jars to the lib folder")

val gmql_lib_path = "conf/gmql_lib/"


copyJarsTask := {
  val jar_name = "GMQL-Cli-" + gmql_version + "-jar-with-dependencies.jar"

  //  TODO change file check with the file attributes
  //  Files.readAttributes(Paths.get(pathStr), classOf[BasicFileAttributes]).creationTime
  //  Files.getFileAttributeView(Paths.get(pathStr), classOf[BasicFileAttributeView]).readAttributes.lastModifiedTime
  //  and size

  val folder = new File(gmql_lib_path)
  (managedClasspath in Compile).value.files.foreach(f => {
    if (f.getName == jar_name)
      if (!FileUtils.contentEquals(f, folder / "GMQL-Cli.jar")) {
        println("Copying " + jar_name + " to " + gmql_lib_path)
        IO.copyFile(f, folder / "GMQL-Cli.jar")
      }
  })



  //  // modify the executor.xml to match the downloaded CLI jar
  //
  //  val modifyCLIJarRule = new RewriteRule {
  //    var changed = false
  //    override def transform(n: Node): Seq[Node] = n match {
  //      case elem: Elem if elem.label == "property" && elem.attribute("name").get.head.text == "CLI_JAR_NAME" && elem.text != jar_name =>
  //        changed = true
  //        elem.copy(child = Text(jar_name))
  //      case elem: Elem if elem.label == "property" && elem.attribute("name").get.head.text == "LIB_DIR_LOCAL" && elem.text != gmql_lib_path =>
  //        changed = true
  //        elem.copy(child = Text(gmql_lib_path))
  //      case n => n
  //    }
  //  }
  //
  //  val executor_xml_path = "./conf/gmql_conf/executor.xml"
  //  println("Changing the executor.xml at " + executor_xml_path)
  //  val transformer = new RuleTransformer(modifyCLIJarRule)
  //  val executor_xml = XML.loadFile(executor_xml_path)
  //  val new_executor_xml = transformer(executor_xml)
  //  if (modifyCLIJarRule.changed) {
  //    XML.save(executor_xml_path, new_executor_xml)
  //  }
}

compile in Compile <<= (compile in Compile).dependsOn(copyJarsTask)

cleanFiles <+= baseDirectory { base => base / gmql_lib_path }