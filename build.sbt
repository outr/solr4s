name := "solr4s"
organization := "com.outr"
version := "1.0.11-SNAPSHOT"
scalaVersion := "2.13.0"
crossScalaVersions := List("2.13.0", "2.12.8", "2.11.12")
parallelExecution in Test := false
fork := true
scalacOptions ++= Seq("-unchecked", "-deprecation")
updateOptions in ThisBuild := updateOptions.value.withLatestSnapshots(false)

publishTo in ThisBuild := sonatypePublishTo.value
sonatypeProfileName in ThisBuild := "com.outr"
publishMavenStyle in ThisBuild := true
licenses in ThisBuild := Seq("MIT" -> url("https://github.com/outr/solr4s/blob/master/LICENSE"))
sonatypeProjectHosting in ThisBuild := Some(xerial.sbt.Sonatype.GitHubHosting("outr", "solr4s", "matt@outr.com"))
homepage in ThisBuild := Some(url("https://github.com/outr/solr4s"))
scmInfo in ThisBuild := Some(
  ScmInfo(
    url("https://github.com/outr/solr4s"),
    "scm:git@github.com:outr/solr4s.git"
  )
)
developers in ThisBuild := List(
  Developer(id="darkfrog", name="Matt Hicks", email="matt@matthicks.com", url=url("http://matthicks.com"))
)

val youiVersion = "0.11.16"

val scalaTestVersion = "3.1.0-SNAP13"

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  "io.youi" %% "youi-client" % youiVersion,
  "org.scalatest" %% "scalatest" % scalaTestVersion % "test"
)

testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oF")
parallelExecution in ThisBuild := false