name := """dsagnier-magbani-projet-mobilite"""
organization := "com.example"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.12.4"

libraryDependencies += guice

libraryDependencies += "javax.xml.bind" % "jaxb-api" % "2.1"

libraryDependencies ++= Seq(
  ws
)
