// Uses SpinalHDL v0.10.11    
// git head : 1b00bb4bd8ebf1bc16dc51836289e6fe86f36b2d

name := "J1SoC"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.github.spinalhdl" % "spinalhdl-core_2.11" % "0.10.16",
  "com.github.spinalhdl" % "spinalhdl-lib_2.11" % "0.10.16"
)

