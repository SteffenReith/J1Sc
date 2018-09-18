// The scala version needed for Spinal
scalaVersion := "2.11.8"

// Added the spinal libraries and a UART-driver
libraryDependencies ++= Seq(
  "com.github.spinalhdl" % "spinalhdl-core_2.11" % "latest.release",
  "com.github.spinalhdl" % "spinalhdl-lib_2.11" % "latest.release",
  "com.github.spinalhdl" % "spinalhdl-sim_2.11" % "latest.release",
  "org.scream3r" % "jssc" % "2.8.0"
)

// Add the plugin the SpinalSim
addCompilerPlugin("org.scala-lang.plugins" % "scala-continuations-plugin_2.11.6" % "1.0.2")
scalacOptions += "-P:continuations:enable"
fork := true

// Setting for the build process
lazy val verilogGenerator = settingKey[String]("Name of the generating class")
lazy val verilogToplevel  = settingKey[String]("Name of the toplevel module (also assumes filename <toplevel>.v)")
lazy val verilogDir       = settingKey[String]("Output directory for RTL")

lazy val genVerilog = taskKey[Unit]("Generate verilog code using SpinalHDL")
lazy val synthYosys = taskKey[Unit]("Synthesize verilog with yosys")
lazy val icePnr     = taskKey[Unit]("Place and route with Icestorm toolchain")
lazy val iceClean   = taskKey[Unit]("Deletes files produced by the build, such as generated sources, compiled classes, and task caches.")

// Clean all generated files
iceClean := {

  val baseDir  = baseDirectory.value
  val toplevel = verilogToplevel.value
  val gendir   = "gen/src/verilog"

  // Print a debug message
  println("[sbt-info] Remove all generated files")

  // Start to remove the files
  Process("rm" :: "-f" :: s"${gendir}/${toplevel}.v" :: Nil, baseDir) !;
  Process("rm" :: "-f" :: s"${gendir}/${toplevel}.asc ${gendir}/${toplevel}.bin ${gendir}/${toplevel}.blif cpu0.yaml" :: Nil, baseDir) !;
  Process("rm" :: "-f" :: s"${gendir}/${toplevel}.v_toplevel_coreArea_cpu_mainMem_ramList_0.bin" :: Nil, baseDir) !;
  Process("rm" :: "-f" :: s"${gendir}/${toplevel}.v_toplevel_coreArea_cpu_mainMem_ramList_1.bin" :: Nil, baseDir) !;
  Process("rm" :: "-fR" :: s"target/ project/target" :: Nil, baseDir) !;

}

// Run the scala program to generate the verilog files
genVerilog := {

  val toplevel = verilogToplevel.value
  val outDir   = "gen/src/verilog"

  // Print a debug message
  println("[sbt-info] Generate verilog code")

  // Run the main-class
  Def.taskDyn[Unit] {
    (runMain in Compile).toTask(s" ${toplevel} --verilog -o ${outDir}")
  }

}

// Do synthesize step
synthYosys :=  {

  // Synthesize the project first
  genVerilog.value

  val toplevel = verilogToplevel.value
  val baseDir  = baseDirectory.value
  val outDir   = baseDirectory.value / verilogDir.value
  val yosysDir = "src/main/lattice/IcoBoard"

  // Print a debug message
  println("[sbt-info] Synthesize netlist using yosys")

  // Extremely ugly workaround because yosys don't supports a path for $readmemb
  Process("cp" :: s"${outDir}/${toplevel}.v_toplevel_coreArea_cpu_mainMem_ramList_0.bin" :: s"${baseDir}" :: Nil, baseDir) !;
  Process("cp" :: s"${outDir}/${toplevel}.v_toplevel_coreArea_cpu_mainMem_ramList_1.bin" :: s"${baseDir}" :: Nil, baseDir) !;

  // Do the synthesis using yosys to generate a netlist out of the verilog code
  Process("yosys" :: "-q" :: s"${baseDir}/${yosysDir}/${toplevel}.ys" :: Nil, baseDir) !;

  // Remove the temporarily copied files
  Process("rm" :: s"${baseDir}/${toplevel}.v_toplevel_coreArea_cpu_mainMem_ramList_0.bin" :: Nil, baseDir) !;
  Process("rm" :: s"${baseDir}/${toplevel}.v_toplevel_coreArea_cpu_mainMem_ramList_1.bin" :: Nil, baseDir) !;

}

// Do place, route and check using the IceStorm toolchain
icePnr := {

  // Synthesize first
  synthYosys.value

  val toplevel    = verilogToplevel.value
  val outDir      = baseDirectory.value / verilogDir.value
  val latticePath = baseDirectory.value / "src/main/lattice/IcoBoard"

  // Print a debug message
  println("[sbt-info] Do place, route, check and generate a bit-file using the IceStorm toolchain")

  // Do the place and route, check the result and generate a bit file
  Process("arachne-pnr" :: "-p" :: s"${latticePath}/${toplevel}.pcf" :: "-d" :: "8k" :: "--max-passes" :: "600" :: s"${toplevel}.blif" :: "-o" :: s"${toplevel}.asc" :: Nil, outDir) !;
  Process("icetime" :: "-tmd" :: "hx8k" :: "-c 40" :: s"${toplevel}.asc" :: Nil, outDir) !;
  Process("icepack" :: s"${toplevel}.asc" :: s"${toplevel}.bin" :: Nil, outDir) !;

}

lazy val J1Sc = RootProject(uri("git://github.com/SteffenReith/J1Sc.git"))

// Default settings
lazy val root = (project in file(".")).settings( organization     := "com.gitHub.J1Sc",
                                                 scalaVersion     := "2.11.8",
                                                 version          := "0.1",
                                                 name             := "J1Sc",
                                                 verilogGenerator := "J1Ico",
                                                 verilogToplevel  := "J1Ico",
                                                 verilogDir       := "gen/src/verilog")
