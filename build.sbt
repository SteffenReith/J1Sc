name := "J1Sc"

organization := "com.github.J1Sc"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.github.spinalhdl" % "spinalhdl-core_2.11" % "1.1.1",
  "com.github.spinalhdl" % "spinalhdl-lib_2.11" % "1.1.1",
  "com.github.spinalhdl" % "spinalhdl-sim_2.11" % "1.1.1",
  "org.scream3r" % "jssc" % "2.8.0"
)

addCompilerPlugin("org.scala-lang.plugins" % "scala-continuations-plugin_2.11.6" % "1.0.2")
scalacOptions += "-P:continuations:enable"
fork := true

