name := "J1Sc"

organization := "com.github.J1Sc"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.github.spinalhdl" % "spinalhdl-core_2.11" % "1.0.5",
  "com.github.spinalhdl" % "spinalhdl-lib_2.11" % "1.0.5"
)

addCompilerPlugin("org.scala-lang.plugins" % "scala-continuations-plugin_2.11.6" % "1.0.2")
scalacOptions += "-P:continuations:enable"
fork := true

