import sbt.ConflictWarning

name := "GMQL-REST"

version := "1.1"

lazy val `GMQL-REST` = (project in file(".")).enablePlugins(PlayScala, SbtWeb)
//lazy val `gmql_rest2` = (project in file(".")).enablePlugins(PlayScala, PlayEbean, PlayJava)

scalaVersion := "2.11.8"
//scalacOptions += "-feature"

//updated to last version of h2
libraryDependencies += "com.h2database" % "h2" % "1.4.192"


libraryDependencies ++= Seq(
//  evolutions,
//  jdbc,
  cache,
  ws,
  //  "org.webjars" % "jquery" % "2.2.1",
  specs2 % Test)


libraryDependencies += "com.typesafe.play" %% "play-slick" % "1.1.1"
libraryDependencies += "com.typesafe.play" %% "play-slick-evolutions" % "1.1.1"


libraryDependencies += "joda-time" % "joda-time" % "2.7"
libraryDependencies += "org.joda" % "joda-convert" % "1.7"
libraryDependencies += "com.github.tototoshi" %% "slick-joda-mapper" % "2.0.0"


unmanagedResourceDirectories in Test <+= baseDirectory(_ / "target/web/public/test")

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

libraryDependencies += "com.github.scala-incubator.io" %% "scala-io-file" % "0.4.3"

//boot strap less
//libraryDependencies += "org.webjars" % "bootstrap" % "3.3.4"

libraryDependencies ++= Seq(
  "org.webjars" %% "webjars-play" % "2.4.0-2"
  , "org.webjars" % "bootstrap" % "3.3.7"
  , "org.webjars" % "jquery" % "2.2.4"
  , "org.webjars" % "jquery-ui" % "1.12.0"
  //  , "org.webjars" % "jquery-ui" % "1.10.4" //-> has also widget


  //  , "org.webjars" % "ace" % "1.2.3"
  , "org.webjars" % "bootstrapvalidator" % "0.5.3"
  //  ,"org.webjars" % "bootstrap-treeview" % "1.2.0"
  //  , "org.webjars" % "fancytree" % "2.3.0"
  //  , "org.webjars" % "jquery-cookie" % "1.4.1-1"
  , "org.webjars" % "js-cookie" % "2.1.0"
  //  , "org.webjars" % "bootstrap-multiselect" % "0.9.13" //USE alternative
  , "org.webjars" % "bootstrap-select" % "1.9.4"
  , "org.webjars" % "jquery-file-upload" % "9.10.1" // THERE IS A NEW VERSION
  /*, "org.webjars" % "bootstrap-filestyle" % "1.1.2"*/
  // USE from source code latest release
  , "org.webjars" % "bootstrap-notify" % "3.1.3"
  , "org.webjars" % "datatables" % "1.10.15"
)


libraryDependencies += "com.jsuereth" %% "scala-arm" % "2.0"


libraryDependencies += "com.typesafe.play" %% "play-mailer" % "4.0.0"



includeFilter in(Assets, LessKeys.less) := "*.less"

excludeFilter in(Assets, LessKeys.less) := "_*.less"

resolvers += Resolver.mavenLocal

//libraryDependencies += "org.apache.lucene" % "lucene-queryparser" % "4.7.0"
//libraryDependencies += "com.sun.jersey" % "jersey-client" % "1.18.1"
libraryDependencies += "com.sun.jersey.contribs" % "jersey-multipart" % "1.9"
libraryDependencies += "com.sun.jersey" % "jersey-core" % "1.9"
libraryDependencies += "org.eclipse.persistence" % "eclipselink" % "2.6.3"



// http://mvnrepository.com/artifact/io.spray/spray-json_2.10
//libraryDependencies += "io.spray" % "spray-json_2.10" % "1.3.2"


//libraryDependencies += "orchestrator.genomics" % "orchestrator" % "7.1"
//libraryDependencies += "it.polimi.genomics" % "GMQL-Cli" % "2.0"
//libraryDependencies += "it.polimi.genomics" % "GMQL-Spark" % "3.0"
//libraryDependencies += "it.polimi.genomics" % "GMQL-Server" % "2.0"
//libraryDependencies += "it.polimi.genomics" % "GMQL-Core" % "2.0"
//libraryDependencies += "it.polimi.genomics" % "Compiler" % "2.0"
//libraryDependencies += "it.polimi.genomics" % "GMQL-R" % "3.0"
//libraryDependencies += "it.polimi.genomics" % "GMQL-Repository" % "1.0"
//libraryDependencies += "it.polimi.genomics" % "GMQL-Flink" % "2.0"
//libraryDependencies += "it.polimi.genomics" % "GMQL-SManager" % "2.0"

//libraryDependencies += "orchestrator.genomics" % "orchestrator" % "7.1"
libraryDependencies += "it.polimi.genomics" % "Compiler" % "1.0-SNAPSHOT"
libraryDependencies += "it.polimi.genomics" % "GMQL-Core" % "1.0-SNAPSHOT"
//libraryDependencies += "it.polimi.genomics" % "GMQL-Flink" % "3.0"
libraryDependencies += "it.polimi.genomics" % "GMQL-Repository" % "1.0-SNAPSHOT"
libraryDependencies += "it.polimi.genomics" % "GMQL-Server" % "1.0-SNAPSHOT"
libraryDependencies += "it.polimi.genomics" % "GMQL-SManager" % "1.0-SNAPSHOT"
libraryDependencies += "it.polimi.genomics" % "GMQL-Spark" % "1.0-SNAPSHOT"


//  [error]    com.typesafe.play:play-ws _2.11, _2.10


//play 2.5.0 will support injected routes as default
routesGenerator := InjectedRoutesGenerator

conflictWarning := ConflictWarning.disable



libraryDependencies +=    "io.swagger" %% "swagger-play2" % "1.5.1"
libraryDependencies += "io.swagger" % "swagger-core" % "1.5.10"

//dependencyOverrides ++= Set(
//  "com.fasterxml.jackson.core" % "jackson-databind" % "2.6.7",
//  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.6.7"
//)




//libraryDependencies += "com.wordnik" %% "swagger-play2" % "1.3.12"


//libraryDependencies ~= { _.map(_.exclude("org.slf4j", "slf4j-log4j12")) }
//libraryDependencies ~= { _.map(_.exclude("com.fasterxml.jackson.module", "jackson-module-scala_2.10")) }
